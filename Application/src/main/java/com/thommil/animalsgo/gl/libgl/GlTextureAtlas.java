package com.thommil.animalsgo.gl.libgl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.thommil.animalsgo.Settings;
import com.thommil.animalsgo.gl.ui.Sprite;
import com.thommil.animalsgo.utils.ByteBufferPool;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class GlTextureAtlas {

    private static final String TAG = "A_GO/GlTextureAtlas";

    private final Map<String, SpriteTemplate> mSpriteMap = new HashMap<>();
    private GlTexture mTexture;

    public GlTextureAtlas(final Context context, final int id){
        this(context, id, new GlTexture(){});

    }

    public GlTextureAtlas(final Context context, final int id, final GlTexture glTextureTemplate){
        parseXML(context, id, glTextureTemplate);

    }

    public void allocate(){
        if(mTexture != null){
            mTexture.bind().allocate().configure();
        }
    }

    public GlTexture getTexture() {
        return mTexture;
    }

    public Sprite createSprite(final String name, final int id){
        final SpriteTemplate template = mSpriteMap.get(name);
        return new Sprite(id, mTexture, template.x, template.y, template.width, template.height, 1f, 1f);
    }

    public void free(){
        if(mTexture != null){
            mTexture.free();
        }
    }

    private void parseXML(final Context context, final int id, final GlTexture glTextureTemplate) {
        //Log.d(TAG, "generateSpriteMapFromXml("+name+")");
        try {
            final XmlPullParser parser = context.getResources().getXml(id);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG){
                    if(parser.getName().equals("TextureAtlas")){
                        final String textureFile = parser.getAttributeValue(null, "imagePath");
                        if(textureFile != null) {
                            generateTexture(context, textureFile, glTextureTemplate);
                        }
                        else throw new XmlPullParserException("Missing name attribute in <TextureAtlas/>");
                    }
                    else if(parser.getName().equals("SubTexture")){
                        final String name = parser.getAttributeValue(null, "name");
                        final int x = Integer.parseInt(parser.getAttributeValue(null, "x"));
                        final int y = Integer.parseInt(parser.getAttributeValue(null, "y"));
                        final int width = Integer.parseInt(parser.getAttributeValue(null, "width"));
                        final int height = Integer.parseInt(parser.getAttributeValue(null, "height"));
                        final float pivotX = Float.parseFloat(parser.getAttributeValue(null, "pivotX"));
                        final float pivotY = Float.parseFloat(parser.getAttributeValue(null, "pivotY"));
                        final Sprite sprite = new Sprite(name.hashCode(), mTexture, x, y, width, height);
                        sprite.setOrigin(width * pivotX, height * pivotY);
                        mSpriteMap.put(name, new SpriteTemplate(name, x, y, width, height, pivotX, pivotY));
                    }
                }

                eventType = parser.next();
            }
        }catch(IOException ioe){
            throw new RuntimeException("Failed to load xml atlas : " + ioe);
        }catch(XmlPullParserException xfpe){
            throw new RuntimeException("Failed to load xml atlas : " + xfpe);
        }
    }

    private static class SpriteTemplate {
        final public String name;
        final public int x;
        final public int y;
        final public int width;
        final public int height;
        final public float pivotX;
        final public float pivotY;

        public SpriteTemplate(String name, int x, int y, int width, int height, float pivotX, float pivotY) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
        }
    }

    private void generateTexture(final Context context, final String textureFile, final GlTexture glTextureTemplate) {
        //Log.d(TAG, "generateTextureMapFromXml("+textureFile+")");
        InputStream in = null;
        try {
            in = context.getResources().getAssets().open(Settings.ASSETS_TEXTURES_PATH + textureFile);
            mTexture = new GLTextureDecorator(BitmapFactory.decodeStream(in), glTextureTemplate);
        }catch(IOException ioe){
            throw new RuntimeException("Texture load error : " + textureFile);
        }finally {
            try{
                in.close();
            }catch (IOException ioe){
                Log.e(TAG, ioe.toString());
            }
        }
    }


    private static class GLTextureDecorator extends GlTexture{

        private GlTexture mGlTextureTemplate;
        private Bitmap mImage;

        public GLTextureDecorator(final Bitmap image, final GlTexture glTextureTemplate){
            mGlTextureTemplate = glTextureTemplate;
            mImage = image;
        }

        @Override
        public ByteBuffer getBytes() {
            final ByteBuffer imageBuffer = ByteBufferPool.getInstance().getDirectByteBuffer(mImage.getByteCount());
            mImage.copyPixelsToBuffer(imageBuffer);
            mImage.recycle();
            return imageBuffer;
        }

        @Override
        public int getHeight() {
            return mImage.getHeight();
        }

        @Override
        public int getWidth() {
            return mImage.getWidth();
        }

        @Override
        public int getTarget() {
            return mGlTextureTemplate.getTarget();
        }

        @Override
        public int getFormat() {
            return mGlTextureTemplate.getFormat();
        }

        @Override
        public int getType() {
            return mGlTextureTemplate.getType();
        }

        @Override
        public int getCompressionFormat() {
            return mGlTextureTemplate.getCompressionFormat();
        }

        @Override
        public int getWrapMode(int axeId) {
            return mGlTextureTemplate.getWrapMode(axeId);
        }

        @Override
        public int getMagnificationFilter() {
            return mGlTextureTemplate.getMagnificationFilter();
        }

        @Override
        public int getMinificationFilter() {
            return mGlTextureTemplate.getMinificationFilter();
        }

        @Override
        public int getSize() {
            return mImage.getByteCount();
        }

        @Override
        public int getLevel() {
            return mGlTextureTemplate.getLevel();
        }
    }

    /*******************************************************************************
     * Copyright 2011 See AUTHORS file.
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *   http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     ******************************************************************************/


    /** Defines a rectangular area of a texture. The coordinate system used has its origin in the upper left corner with the x-axis
     * pointing to the right and the y axis pointing downwards.
     * @author mzechner
     * @author Nathan Sweet */
    public static class TextureRegion {
        GlTexture texture;
        float u, v;
        float u2, v2;
        int regionWidth, regionHeight;

        /** Constructs a region with no texture and no coordinates defined. */
        public TextureRegion () {
        }

        /** Constructs a region the size of the specified texture. */
        public TextureRegion (GlTexture texture) {
            if (texture == null) throw new IllegalArgumentException("texture cannot be null.");
            this.texture = texture;
            setRegion(0, 0, texture.getWidth(), texture.getHeight());
        }

        /** @param width The width of the texture region. May be negative to flip the sprite when drawn.
         * @param height The height of the texture region. May be negative to flip the sprite when drawn. */
        public TextureRegion (GlTexture texture, int width, int height) {
            this.texture = texture;
            setRegion(0, 0, width, height);
        }

        /** @param width The width of the texture region. May be negative to flip the sprite when drawn.
         * @param height The height of the texture region. May be negative to flip the sprite when drawn. */
        public TextureRegion (GlTexture texture, int x, int y, int width, int height) {
            this.texture = texture;
            setRegion(x, y, width, height);
        }

        public TextureRegion (GlTexture texture, float u, float v, float u2, float v2) {
            this.texture = texture;
            setRegion(u, v, u2, v2);
        }

        /** Constructs a region with the same texture and coordinates of the specified region. */
        public TextureRegion (TextureRegion region) {
            setRegion(region);
        }

        /** Constructs a region with the same texture as the specified region and sets the coordinates relative to the specified region.
         * @param width The width of the texture region. May be negative to flip the sprite when drawn.
         * @param height The height of the texture region. May be negative to flip the sprite when drawn. */
        public TextureRegion (TextureRegion region, int x, int y, int width, int height) {
            setRegion(region, x, y, width, height);
        }

        /** Sets the texture and sets the coordinates to the size of the specified texture. */
        public void setRegion (GlTexture texture) {
            this.texture = texture;
            setRegion(0, 0, texture.getWidth(), texture.getHeight());
        }

        /** @param width The width of the texture region. May be negative to flip the sprite when drawn.
         * @param height The height of the texture region. May be negative to flip the sprite when drawn. */
        public void setRegion (int x, int y, int width, int height) {
            float invTexWidth = 1f / texture.getWidth();
            float invTexHeight = 1f / texture.getHeight();
            setRegion(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight);
            regionWidth = Math.abs(width);
            regionHeight = Math.abs(height);
        }

        public void setRegion (float u, float v, float u2, float v2) {
            int texWidth = texture.getWidth(), texHeight = texture.getHeight();
            regionWidth = Math.round(Math.abs(u2 - u) * texWidth);
            regionHeight = Math.round(Math.abs(v2 - v) * texHeight);

            // For a 1x1 region, adjust UVs toward pixel center to avoid filtering artifacts on AMD GPUs when drawing very stretched.
            if (regionWidth == 1 && regionHeight == 1) {
                float adjustX = 0.25f / texWidth;
                u += adjustX;
                u2 -= adjustX;
                float adjustY = 0.25f / texHeight;
                v += adjustY;
                v2 -= adjustY;
            }

            this.u = u;
            this.v = v;
            this.u2 = u2;
            this.v2 = v2;
        }

        /** Sets the texture and coordinates to the specified region. */
        public void setRegion (TextureRegion region) {
            texture = region.texture;
            setRegion(region.u, region.v, region.u2, region.v2);
        }

        /** Sets the texture to that of the specified region and sets the coordinates relative to the specified region. */
        public void setRegion (TextureRegion region, int x, int y, int width, int height) {
            texture = region.texture;
            setRegion(region.getRegionX() + x, region.getRegionY() + y, width, height);
        }

        public GlTexture getTexture () {
            return texture;
        }

        public void setTexture (GlTexture texture) {
            this.texture = texture;
        }

        public float getU () {
            return u;
        }

        public void setU (float u) {
            this.u = u;
            regionWidth = Math.round(Math.abs(u2 - u) * texture.getWidth());
        }

        public float getV () {
            return v;
        }

        public void setV (float v) {
            this.v = v;
            regionHeight = Math.round(Math.abs(v2 - v) * texture.getHeight());
        }

        public float getU2 () {
            return u2;
        }

        public void setU2 (float u2) {
            this.u2 = u2;
            regionWidth = Math.round(Math.abs(u2 - u) * texture.getWidth());
        }

        public float getV2 () {
            return v2;
        }

        public void setV2 (float v2) {
            this.v2 = v2;
            regionHeight = Math.round(Math.abs(v2 - v) * texture.getHeight());
        }

        public int getRegionX () {
            return Math.round(u * texture.getWidth());
        }

        public void setRegionX (int x) {
            setU(x / (float)texture.getWidth());
        }

        public int getRegionY () {
            return Math.round(v * texture.getHeight());
        }

        public void setRegionY (int y) {
            setV(y / (float)texture.getHeight());
        }

        /** Returns the region's width. */
        public int getRegionWidth () {
            return regionWidth;
        }

        public void setRegionWidth (int width) {
            if (isFlipX()) {
                setU(u2 + width / (float)texture.getWidth());
            } else {
                setU2(u + width / (float)texture.getWidth());
            }
        }

        /** Returns the region's height. */
        public int getRegionHeight () {
            return regionHeight;
        }

        public void setRegionHeight (int height) {
            if (isFlipY()) {
                setV(v2 + height / (float)texture.getHeight());
            } else {
                setV2(v + height / (float)texture.getHeight());
            }
        }

        public void flip (boolean x, boolean y) {
            if (x) {
                float temp = u;
                u = u2;
                u2 = temp;
            }
            if (y) {
                float temp = v;
                v = v2;
                v2 = temp;
            }
        }

        public boolean isFlipX () {
            return u > u2;
        }

        public boolean isFlipY () {
            return v > v2;
        }

        /** Offsets the region relative to the current region. Generally the region's size should be the entire size of the texture in
         * the direction(s) it is scrolled.
         * @param xAmount The percentage to offset horizontally.
         * @param yAmount The percentage to offset vertically. This is done in texture space, so up is negative. */
        public void scroll (float xAmount, float yAmount) {
            if (xAmount != 0) {
                float width = (u2 - u) * texture.getWidth();
                u = (u + xAmount) % 1;
                u2 = u + width / texture.getWidth();
            }
            if (yAmount != 0) {
                float height = (v2 - v) * texture.getHeight();
                v = (v + yAmount) % 1;
                v2 = v + height / texture.getHeight();
            }
        }

        /** Helper function to create tiles out of this TextureRegion starting from the top left corner going to the right and ending at
         * the bottom right corner. Only complete tiles will be returned so if the region's width or height are not a multiple of the
         * tile width and height not all of the region will be used. This will not work on texture regions returned form a TextureAtlas
         * that either have whitespace removed or where flipped before the region is split.
         *
         * @param tileWidth a tile's width in pixels
         * @param tileHeight a tile's height in pixels
         * @return a 2D array of TextureRegions indexed by [row][column]. */
        public TextureRegion[][] split (int tileWidth, int tileHeight) {
            int x = getRegionX();
            int y = getRegionY();
            int width = regionWidth;
            int height = regionHeight;

            int rows = height / tileHeight;
            int cols = width / tileWidth;

            int startX = x;
            TextureRegion[][] tiles = new TextureRegion[rows][cols];
            for (int row = 0; row < rows; row++, y += tileHeight) {
                x = startX;
                for (int col = 0; col < cols; col++, x += tileWidth) {
                    tiles[row][col] = new TextureRegion(texture, x, y, tileWidth, tileHeight);
                }
            }

            return tiles;
        }

        /** Helper function to create tiles out of the given {@link GlTexture} starting from the top left corner going to the right and
         * ending at the bottom right corner. Only complete tiles will be returned so if the texture's width or height are not a
         * multiple of the tile width and height not all of the texture will be used.
         *
         * @param texture the Texture
         * @param tileWidth a tile's width in pixels
         * @param tileHeight a tile's height in pixels
         * @return a 2D array of TextureRegions indexed by [row][column]. */
        public static TextureRegion[][] split (GlTexture texture, int tileWidth, int tileHeight) {
            TextureRegion region = new TextureRegion(texture);
            return region.split(tileWidth, tileHeight);
        }
    }
}
