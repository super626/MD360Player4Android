package com.asha.vrlib.plugins;

import android.content.Context;

import com.asha.vrlib.MD360Director;
import com.asha.vrlib.MD360Program;
import com.asha.vrlib.model.MDMainPluginBuilder;
import com.asha.vrlib.model.MDPosition;
import com.asha.vrlib.objects.MDAbsObject3D;
import com.asha.vrlib.strategy.projection.ProjectionModeManager;
import com.asha.vrlib.texture.MD360Texture;

import static com.asha.vrlib.common.GLUtil.glCheck;

/**
 * Created by hzqiujiadi on 16/7/22.
 * hzqiujiadi ashqalcn@gmail.com
 */
public class MDVRInterweavePlugin extends MDAbsPlugin {

    private MD360Program mProgram;

    private MD360Texture mTexture;

    private ProjectionModeManager mProjectionModeManager;

    public MDVRInterweavePlugin(MDMainPluginBuilder builder) {
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
                "varying vec2 v_TexCoordinate;\n" +
                "void main(){ \n" +
                "float coordy = v_TexCoordinate.y;\n" +
                "float idx = floor(gl_FragCoord.x);\n" +
                "float factor = mod(idx, 2.0);\n" +
                "coordy = (factor == 0.0 ? 0.5 * v_TexCoordinate.y : 0.5 * v_TexCoordinate.y + 0.5); \n" +
                "vec2 coord = vec2(v_TexCoordinate.x, coordy);\n" +
                "gl_FragColor = texture2D(u_Texture, coord); \n" +
                "}";
        mProgram.build(strVs, strFs);
        //mProgram.build(context);
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
        glCheck("MDPanoramaPlugin mProgram use");

        mTexture.texture(mProgram);

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
