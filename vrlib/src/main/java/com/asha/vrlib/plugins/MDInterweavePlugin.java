package com.asha.vrlib.plugins;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.asha.vrlib.MD360Director;
import com.asha.vrlib.MD360Program;
import com.asha.vrlib.common.GLUtil;
import com.asha.vrlib.model.MDMainPluginBuilder;
import com.asha.vrlib.model.MDPosition;
import com.asha.vrlib.objects.MDAbsObject3D;
import com.asha.vrlib.strategy.projection.ProjectionModeManager;
import com.asha.vrlib.texture.MD360Texture;

import javax.microedition.khronos.opengles.GL;

import static com.asha.vrlib.common.GLUtil.glCheck;

/**
 * 交织
 */
public class MDInterweavePlugin extends MDAbsPlugin {

    private MD360Program mProgram;

    private MD360Texture mTexture;

    private int mStepSizeXHandle;
    private int mStepSizeYHandle;
    private int mScaleHandle;

    private ProjectionModeManager mProjectionModeManager;

    public static float sharpness = 0.5f;

    public MDInterweavePlugin(MDMainPluginBuilder builder) {
        mTexture = builder.getTexture();
        mProgram = new MD360Program(builder.getContentType());
        mProjectionModeManager = builder.getProjectionModeManager();
    }

    @Override
    public void init(Context context) {
        String strVs, strFs;
        strVs = "uniform mat4 u_MVPMatrix;\n" +
                "attribute vec4 a_Position;\n" +
                "attribute vec2 a_TexCoordinate;\n" +
                "varying vec2 v_TexCoordinate;\n" +
                "void main() {\n" +
                "gl_Position = u_MVPMatrix * a_Position;\n" +
                "v_TexCoordinate = a_TexCoordinate;\n" +
                "}";
        strFs = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES u_Texture;\n" +
                "uniform float u_stepSizeX; \n" +
                "uniform float u_stepSizeY; \n" +
                "uniform float u_scale; \n" +
                "varying vec2 v_TexCoordinate;\n" +
                "void main(){ \n" +
                "float coordx = v_TexCoordinate.x;\n" +
                "float idx = floor(gl_FragCoord.x);\n" +
                "float factor = mod(idx, 2.0);\n" +
                "coordx = (factor == 0.0 ? 0.5 * v_TexCoordinate.x : 0.5 * v_TexCoordinate.x + 0.5); \n" +
                "vec2 coord = vec2(coordx, v_TexCoordinate.y);\n" +
                "if (u_scale == 0.0) \n" +
                "gl_FragColor = texture2D(u_Texture, coord); \n" +
                "else { \n" +
                "vec3 nbr_color = vec3(0.0, 0.0, 0.0);\n" +
                "vec4 color = texture2D(u_Texture, coord);\n" +
                "float mincoordx = (factor == 0.0 ? 0.0 : 0.5); \n" +
                "float maxcoordx = (factor == 0.0 ? 0.5 : 1.0); \n" +
                "coord.x = coordx - 0.5 * u_stepSizeX;////v_TexCoordinate.x - u_stepSizeX;//\n" +
                "coord.x = clamp(coord.x, mincoordx, maxcoordx); \n" +
                "coord.y = v_TexCoordinate.y - u_stepSizeY;//v_TexCoordinate.y;////\n" +
                "nbr_color += texture2D(u_Texture, coord).rgb - color.rgb;\n" +
                "coord.x = coordx - u_stepSizeX;////v_TexCoordinate.x + u_stepSizeX;//\n" +
                "coord.x = clamp(coord.x, mincoordx, maxcoordx); \n" +
                "coord.y = v_TexCoordinate.y + 0.5 * u_stepSizeY;////v_TexCoordinate.y;//\n" +
                "nbr_color += texture2D(u_Texture, coord).rgb - color.rgb;\n" +
                "coord.x = coordx + u_stepSizeX;//v_TexCoordinate.x;////\n" +
                "coord.x = clamp(coord.x, mincoordx, maxcoordx); \n" +
                "coord.y = v_TexCoordinate.y - 0.5 * u_stepSizeY;////v_TexCoordinate.y - u_stepSizeY;//\n" +
                "nbr_color += texture2D(u_Texture, coord).rgb - color.rgb;\n" +
                "coord.x = coordx + u_stepSizeX;////v_TexCoordinate.x;//\n" +
                "coord.x = clamp(coord.x, mincoordx, maxcoordx); \n" +
                "coord.y = v_TexCoordinate.y + 0.5 * u_stepSizeY;////v_TexCoordinate.y + u_stepSizeY;//\n" +
                "nbr_color += texture2D(u_Texture, coord).rgb - color.rgb;\n" +
                "gl_FragColor = vec4(color.rgb - 2.0 * u_scale * nbr_color, color.a);\n" +
                "}\n" +
                "}";
        mProgram.build(strVs, strFs);

        mStepSizeXHandle = mProgram.getUniform("u_stepSizeX");
        mStepSizeYHandle = mProgram.getUniform("u_stepSizeY");
        mScaleHandle = mProgram.getUniform("u_scale");
        mTexture.create();
    }

    @Override
    public void beforeRenderer(int totalWidth, int totalHeight) {

    }

    @Override
    public void renderer(int index, int width, int height, MD360Director director) {

        MDAbsObject3D object3D = mProjectionModeManager.getObject3D();
        // check obj3d
        if (object3D == null) return;

        // Update Projection
        director.updateViewport(width, height);

        // Set our per-vertex lighting program.
        mProgram.use();

        mTexture.texture(mProgram);

        GLES20.glUniform1f(mStepSizeXHandle, 1.0f / width);
        GLES20.glUniform1f(mStepSizeYHandle, 1.0f / height);
        GLES20.glUniform1f(mScaleHandle, sharpness);

        object3D.uploadVerticesBufferIfNeed(mProgram, index);

        object3D.uploadTexCoordinateBufferIfNeed(mProgram, index);

        // Pass in the combined matrix.
        director.shot(mProgram, getModelPosition());
        object3D.draw();

    }

    @Override
    public void destroy() {
        mTexture = null;
    }

    @Override
    protected MDPosition getModelPosition() {
        return mProjectionModeManager.getModelPosition();
    }

    @Override
    protected boolean removable() {
        return false;
    }

}
