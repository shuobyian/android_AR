/*===============================================================================
Copyright (c) 2019 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

package com.vuforia.engine.CoreSamples.app.VirtualButtons;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.Device;
import com.vuforia.ImageTargetResult;
import com.vuforia.Rectangle;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.VirtualButton;
import com.vuforia.VirtualButtonResult;
import com.vuforia.VirtualButtonResultList;
import com.vuforia.Vuforia;
import com.vuforia.engine.CoreSamples.R;
import com.vuforia.engine.CoreSamples.app.ImageTargets.OBJLoader;
import com.vuforia.engine.CoreSamples.app.ImageTargets.OBJParser.ObjParser;
import com.vuforia.engine.SampleApplication.SampleAppRenderer;
import com.vuforia.engine.SampleApplication.SampleAppRendererControl;
import com.vuforia.engine.SampleApplication.SampleApplicationSession;
import com.vuforia.engine.SampleApplication.SampleRendererBase;
import com.vuforia.engine.SampleApplication.utils.CubeShaders;
import com.vuforia.engine.SampleApplication.utils.LineShaders;
import com.vuforia.engine.SampleApplication.utils.SampleUtils;
import com.vuforia.engine.SampleApplication.utils.Teapot;
import com.vuforia.engine.SampleApplication.utils.Texture;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Vector;


/**
 * The renderer class for the Virtual Buttons sample.
 *
 * In the renderFrame() function you can render augmentations to display over the Target
 */
public class VirtualButtonRenderer extends SampleRendererBase implements SampleAppRendererControl
{
    private static final String LOGTAG = "VirtualButtonRenderer";

    private int NUM = 30;

    private final VirtualButtons mActivity;

    private Vector<Texture> mTextures;

    // Object to be rendered
    private final Teapot mTeapot = new Teapot();
    private OBJLoader[] mObj = new OBJLoader[NUM];

    // OpenGL ES 2.0 specific (3D model):
    private int shaderProgramID = 0;
    private int vertexHandle = 0;
    private int textureCoordHandle = 0;
    private int mvpMatrixHandle = 0;
    private int texSampler2DHandle = 0;

    private int lineOpacityHandle = 0;
    private int lineColorHandle = 0;
    private int mvpMatrixButtonsHandle = 0;

    // OpenGL ES 2.0 specific (Virtual Buttons):
    private int vbShaderProgramID = 0;
    private int vbVertexHandle = 0;

    private static final float kTeapotScale = 0.003f, kObjScale = 0.015f;

    // Define the coordinates of the virtual buttons to render the area of action,
    // These values are the same as those in Wood.xml
    static private final float[] LEFT_VB_BUTTON =  {-0.10868f, -0.05352f, -0.07575f, -0.06587f};
    static private final float[] UP_VB_BUTTON =  {-0.04528f, -0.05352f, -0.01235f, -0.06587f};
    static private final float[] RIGHT_VB_BUTTON =  {0.01482f, -0.05352f, 0.04775f, -0.06587f};
    static private final float[] DOWN_VB_BUTTON =  {0.07657f, -0.05352f, 0.10950f, -0.06587f};

    public double foxposition_x = 0.0f;
    public double foxposition_z = 0.0f;

    private float[] apple_height = {5.0f, 5.0f, 4.0f};
    private float[] banana_height = {5.0f, 5.5f};
    private float fox_angle = 0.0f;
    private float butterfly_angle = 0.0f;
    private float fairy_angle = 0.0f;
    private float butterfly_distance = 2.5f;
    private float fairy_distance = 1.0f;

    private int objects;

    private ArrayList[] verticeBuffers = new ArrayList[NUM];
    private ArrayList[] textureBuffers = new ArrayList[NUM];

    // fox 움직임
    public float xmove = 0.0f;
    public float zmove = 0.0f;

    public boolean state_fruit_reset = false;
    public int cnt = 0;
    public boolean state_crash_tree = false;
    public boolean state_crash_tree2 = false;
    public boolean state_butterfly = false;


    private int obj[] = {R.raw.tree, R.raw.tree2, R.raw.fox, R.raw.apple, R.raw.banana, R.raw.butterfly, R.raw.fairy};


    VirtualButtonRenderer(VirtualButtons activity,
        SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mSampleAppRenderer = new SampleAppRenderer(this, mActivity, Device.MODE.MODE_AR, vuforiaAppSession.getVideoMode(), false, 0.01f, 5f);
    }


    public void setActive(boolean active)
    {
        mSampleAppRenderer.setActive(active);
    }

    @Override
    public void initRendering()
    {
        Log.d(LOGTAG, "initRendering");

        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
            : 1.0f);

        // Now generate the OpenGL texture objects and add settings
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
            CubeShaders.CUBE_MESH_VERTEX_SHADER,
            CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
            "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
            "texSampler2D");

        // OpenGL setup for Virtual Buttons
        vbShaderProgramID = SampleUtils.createProgramFromShaderSrc(
            LineShaders.LINE_VERTEX_SHADER, LineShaders.LINE_FRAGMENT_SHADER);

        mvpMatrixButtonsHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
            "modelViewProjectionMatrix");
        vbVertexHandle = GLES20.glGetAttribLocation(vbShaderProgramID,
            "vertexPosition");
        lineOpacityHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
            "opacity");
        lineColorHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
            "color");

        ObjParser[] objParser = new ObjParser[NUM];
        for(int i=0;i<obj.length;i++) {
            objParser[i] = new ObjParser(mActivity);
            try{
                objParser[i].parse(obj[i]);
            } catch(IOException e) {
            }
            objects = objParser[i].getObjectIds().size();
            mObj[i] = new OBJLoader(objParser[i]);
            verticeBuffers[i] = mObj[i].getBuffers(0);
            textureBuffers[i] = mObj[i].getBuffers(2);
        }
    }


    public void updateRenderingPrimitives()
    {
        mSampleAppRenderer.updateRenderingPrimitives();
    }


    // The render function.
    // This function is called from the SampleAppRenderer by using the RenderingPrimitives views.
    // The state is owned by SampleAppRenderer which is controlling its lifecycle.
    // NOTE: State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix)
    {
        mSampleAppRenderer.renderVideoBackground(state);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//        GLES20.glEnable(GLES20.GL_CULL_FACE);
//        GLES20.glCullFace(GLES20.GL_BACK);

            if(!state.getTrackableResults().empty()) {
                // Get the trackable:
                TrackableResult trackableResult = state.getTrackableResults().at(0);
                float[] modelViewMatrix = Tool.convertPose2GLMatrix(
                        trackableResult.getPose()).getData();

                // The image target specific result:
                ImageTargetResult imageTargetResult = (ImageTargetResult) trackableResult;
                VirtualButtonResultList virtualButtonResultList = imageTargetResult.getVirtualButtonResults();

                // Set transformations:
                float[] modelViewProjection = new float[16];
                Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

                // Set the texture used for the teapot model:
                //int textureIndex = 0;

                float vbVertices[] = new float[virtualButtonResultList.size() * 24];
                short vbCounter = 0;

                // Iterate through this targets virtual buttons:
                for (VirtualButtonResult buttonResult : virtualButtonResultList)
                {
                    VirtualButton button = buttonResult.getVirtualButton();

                    int buttonIndex = 0;

                    // Run through button name array to find button index
                    for (int j = 0; j < VirtualButtons.NUM_BUTTONS; ++j)
                    {
                        if (button.getName().compareTo(
                                mActivity.virtualButtons[j]) == 0)
                        {
                            buttonIndex = j;
                            break;
                        }
                    }

                    // If the button is pressed, than use this texture:
                    if (buttonResult.isPressed())
                    {
                        if(buttonIndex==0) {
                            xmove-=0.05f;
                            fox_angle=270;
                        }
                        if(buttonIndex==1) {
                            zmove-=0.05f;
                            fox_angle=180;
                        }
                        if(buttonIndex==2) {
                            xmove+=0.05f;
                            fox_angle=90;
                        }
                        if(buttonIndex==3) {
                            zmove+=0.05f;
                            fox_angle=0;
                        }
                    }

                    // Define the four virtual buttons as Rectangle using the same values as the dataset
                    Rectangle vbRectangle[] = new Rectangle[4];
                    vbRectangle[0] = new Rectangle(LEFT_VB_BUTTON[0], LEFT_VB_BUTTON[1],
                            LEFT_VB_BUTTON[2], LEFT_VB_BUTTON[3]);
                    vbRectangle[1] = new Rectangle(UP_VB_BUTTON[0], UP_VB_BUTTON[1],
                            UP_VB_BUTTON[2], UP_VB_BUTTON[3]);
                    vbRectangle[2] = new Rectangle(RIGHT_VB_BUTTON[0], RIGHT_VB_BUTTON[1],
                            RIGHT_VB_BUTTON[2], RIGHT_VB_BUTTON[3]);
                    vbRectangle[3] = new Rectangle(DOWN_VB_BUTTON[0], DOWN_VB_BUTTON[1],
                            DOWN_VB_BUTTON[2], DOWN_VB_BUTTON[3]);

                    // We add the vertices to a common array in order to have one
                    // single draw call. This is more efficient than having multiple
                    // glDrawArray calls
                    vbVertices[vbCounter] = vbRectangle[buttonIndex].getLeftTopX();
                    vbVertices[vbCounter + 1] = vbRectangle[buttonIndex]
                            .getLeftTopY();
                    vbVertices[vbCounter + 2] = 0.0f;
                    vbVertices[vbCounter + 3] = vbRectangle[buttonIndex]
                            .getRightBottomX();
                    vbVertices[vbCounter + 4] = vbRectangle[buttonIndex]
                            .getLeftTopY();
                    vbVertices[vbCounter + 5] = 0.0f;
                    vbVertices[vbCounter + 6] = vbRectangle[buttonIndex]
                            .getRightBottomX();
                    vbVertices[vbCounter + 7] = vbRectangle[buttonIndex]
                            .getLeftTopY();
                    vbVertices[vbCounter + 8] = 0.0f;
                    vbVertices[vbCounter + 9] = vbRectangle[buttonIndex]
                            .getRightBottomX();
                    vbVertices[vbCounter + 10] = vbRectangle[buttonIndex]
                            .getRightBottomY();
                    vbVertices[vbCounter + 11] = 0.0f;
                    vbVertices[vbCounter + 12] = vbRectangle[buttonIndex]
                            .getRightBottomX();
                    vbVertices[vbCounter + 13] = vbRectangle[buttonIndex]
                            .getRightBottomY();
                    vbVertices[vbCounter + 14] = 0.0f;
                    vbVertices[vbCounter + 15] = vbRectangle[buttonIndex]
                            .getLeftTopX();
                    vbVertices[vbCounter + 16] = vbRectangle[buttonIndex]
                            .getRightBottomY();
                    vbVertices[vbCounter + 17] = 0.0f;
                    vbVertices[vbCounter + 18] = vbRectangle[buttonIndex]
                            .getLeftTopX();
                    vbVertices[vbCounter + 19] = vbRectangle[buttonIndex]
                            .getRightBottomY();
                    vbVertices[vbCounter + 20] = 0.0f;
                    vbVertices[vbCounter + 21] = vbRectangle[buttonIndex]
                            .getLeftTopX();
                    vbVertices[vbCounter + 22] = vbRectangle[buttonIndex]
                            .getLeftTopY();
                    vbVertices[vbCounter + 23] = 0.0f;
                    vbCounter += 24;

                }

                // We only render if there is something on the array
                if (vbCounter > 0)
                {
                    // Render frame around button
                    GLES20.glUseProgram(vbShaderProgramID);
                    GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0, fillBuffer(vbVertices));
                    GLES20.glEnableVertexAttribArray(vbVertexHandle);
                    GLES20.glUniform1f(lineOpacityHandle, 1.0f);
                    GLES20.glUniform3f(lineColorHandle, 1.0f, 1.0f, 1.0f);
                    GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false, modelViewProjection, 0);
                    GLES20.glDrawArrays(GLES20.GL_LINES, 0, virtualButtonResultList.size() * 8);
                    SampleUtils.checkGLError("VirtualButtons drawButton");

                    GLES20.glDisableVertexAttribArray(vbVertexHandle);
                }

                float[] rotationMatrix = new float[16];
                float[] translationMatrix = new float[16];
                float[] modelViewProjectionScaled = new float[16];
                float[] modelViewMatrix_tree = new float[16];
                float[] modelViewMatrix_tree_inverse = new float[16];
                float[] modelViewMatrix_tree2 = new float[16];
                float[] modelViewMatrix_tree2_inverse = new float[16];
                float[] modelViewMatrix_fox = new float[16];
                float[] modelViewMatrix_fox_inverse = new float[16];
                float[] modelViewMatrix_apple = new float[16];
                float[] modelViewMatrix_apple_inverse = new float[16];
                float[] modelViewMatrix_banana = new float[16];
                float[] modelViewMatrix_banana_inverse = new float[16];
                float[] modelViewMatrix_butterfly = new float[16];
                float[] modelViewMatrix_butterfly_inverse = new float[16];
                float[] modelViewMatrix_fairy = new float[16];
                float[] modelViewMatrix_fairy_inverse = new float[16];

                // Scale 3D model
//            Matrix.scaleM(modelViewMatrix, 0, kTeapotScale, kTeapotScale,
//                kTeapotScale);

                Matrix.scaleM(modelViewMatrix, 0, kObjScale, kObjScale, kObjScale);

                // Render 3D model
                GLES20.glUseProgram(shaderProgramID);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                Matrix.setIdentityM(translationMatrix, 0);
                Matrix.setIdentityM(rotationMatrix, 0);
                Matrix.setRotateM(rotationMatrix, 0, 90, 1.0f, 0.0f, 0.0f);
                Matrix.multiplyMM(modelViewMatrix, 0, modelViewMatrix, 0, rotationMatrix, 0);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix, 0);

                // tree
                Matrix.setIdentityM(translationMatrix, 0);
                Matrix.translateM(translationMatrix, 0, 3.0f, 0.0f, -2.0f);
                Matrix.multiplyMM(modelViewMatrix_tree, 0, modelViewMatrix, 0, translationMatrix, 0);
                Matrix.scaleM(modelViewMatrix_tree, 0, 1.0f, 1.0f, 1.0f);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_tree, 0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[0].get(0));
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[0].get(0));
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 1);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[0].getNumObjectVertex(0));
                Matrix.invertM(modelViewMatrix_tree_inverse, 0, modelViewMatrix_tree, 0);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_tree_inverse, 0);

                // tree2
                Matrix.setIdentityM(translationMatrix, 0);
                Matrix.translateM(translationMatrix, 0, -2.0f, 0.0f, -2.0f);
                Matrix.multiplyMM(modelViewMatrix_tree2, 0, modelViewMatrix, 0, translationMatrix, 0);
                Matrix.scaleM(modelViewMatrix_tree2, 0, 1.0f, 1.0f, 1.0f);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_tree2, 0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[1].get(0));
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[1].get(0));
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 2);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[1].getNumObjectVertex(0));
                Matrix.invertM(modelViewMatrix_tree2_inverse, 0, modelViewMatrix_tree2, 0);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_tree2_inverse, 0);

                // fox
                Matrix.setIdentityM(translationMatrix, 0);
                Matrix.translateM(translationMatrix, 0, xmove, 0, zmove);
                Matrix.multiplyMM(modelViewMatrix_fox, 0, modelViewMatrix, 0, translationMatrix, 0);
                Matrix.setIdentityM(rotationMatrix, 0);
                Matrix.setRotateM(rotationMatrix, 0, fox_angle, 0.0f, 1.0f, 0.0f);
                Matrix.multiplyMM(modelViewMatrix_fox, 0, modelViewMatrix_fox, 0, rotationMatrix, 0);
                Matrix.scaleM(modelViewMatrix_fox, 0, 0.3f, 0.3f, 0.3f);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_fox, 0);
                foxposition_x = xmove; foxposition_z = zmove;
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[2].get(0));
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[2].get(0));
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 3);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[2].getNumObjectVertex(0));
                Matrix.invertM(modelViewMatrix_fox_inverse, 0, modelViewMatrix_fox, 0);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_fox_inverse, 0);

                float[][] apple_position = {{1.0f, -2.0f}, {3.0f, -1.0f}, {4.0f, -1.0f}};
                // apple (tree)
                for(int i=0;i<apple_position.length;i++) {
                    Matrix.setIdentityM(translationMatrix, 0);
                    Matrix.translateM(translationMatrix, 0, apple_position[i][0], apple_height[i], apple_position[i][1]);
                    Matrix.multiplyMM(modelViewMatrix_apple, 0, modelViewMatrix, 0, translationMatrix, 0);
                    Matrix.scaleM(modelViewMatrix_apple, 0, 0.09f, 0.09f, 0.09f);
                    Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_apple, 0);
                    GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[3].get(0));
                    GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[3].get(0));
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 4);
                    GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
                    GLES20.glUniform1i(texSampler2DHandle, 0);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[3].getNumObjectVertex(0));
                    Matrix.invertM(modelViewMatrix_apple_inverse, 0, modelViewMatrix_apple, 0);
                    Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_apple_inverse, 0);
                }

                // banana (tree2)
                float[][] banana_position = {{-1.0f, -1.3f}, {-3.0f, -1.0f}};
                for(int i=0;i<banana_position.length;i++) {
                    Matrix.setIdentityM(translationMatrix, 0);
                    Matrix.translateM(translationMatrix, 0, banana_position[i][0], banana_height[i], banana_position[i][1]);
                    Matrix.multiplyMM(modelViewMatrix_banana, 0, modelViewMatrix, 0, translationMatrix, 0);
                    Matrix.setIdentityM(rotationMatrix, 0);
                    Matrix.setRotateM(rotationMatrix, 0, 30, 0.0f, -1.0f, 0.0f);
                    Matrix.multiplyMM(modelViewMatrix_banana, 0, modelViewMatrix_banana, 0, rotationMatrix, 0);
                    Matrix.scaleM(modelViewMatrix_banana, 0, 0.15f, 0.15f, 0.15f);
                    Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_banana, 0);
                    GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[4].get(0));
                    GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[4].get(0));
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 5);
                    GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
                    GLES20.glUniform1i(texSampler2DHandle, 0);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[4].getNumObjectVertex(0));
                    Matrix.invertM(modelViewMatrix_banana_inverse, 0, modelViewMatrix_banana, 0);
                    Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_banana_inverse, 0);
                }

                // butterfly
                Matrix.setIdentityM(translationMatrix, 0);
                Matrix.translateM(translationMatrix, 0, 0.5f, 0.f, 0.0f);
                Matrix.multiplyMM(modelViewMatrix_butterfly, 0, modelViewMatrix, 0, translationMatrix, 0);
                if(state_butterfly) {
                    Matrix.setIdentityM(translationMatrix, 0);
                    Matrix.translateM(translationMatrix, 0, 3.0f, 4.0f, -2.0f);
                    Matrix.multiplyMM(modelViewMatrix_butterfly, 0, modelViewMatrix_butterfly, 0, translationMatrix, 0);
                    Matrix.setIdentityM(rotationMatrix, 0);
                    Matrix.setRotateM(rotationMatrix, 0, butterfly_angle, 0.0f, 1.0f, 0.0f);
                    Matrix.multiplyMM(modelViewMatrix_butterfly, 0, modelViewMatrix_butterfly, 0, rotationMatrix, 0);
                    Matrix.setIdentityM(translationMatrix, 0);
                    Matrix.translateM(translationMatrix, 0, -butterfly_distance, 0.0f, 0.0f);
                    Matrix.multiplyMM(modelViewMatrix_butterfly, 0, modelViewMatrix_butterfly, 0, translationMatrix, 0);
                }
                else {
                    Matrix.setIdentityM(translationMatrix, 0);
                    Matrix.translateM(translationMatrix, 0, -2.0f, 4.0f, -2.0f);
                    Matrix.multiplyMM(modelViewMatrix_butterfly, 0, modelViewMatrix_butterfly, 0, translationMatrix, 0);
                    Matrix.setIdentityM(rotationMatrix, 0);
                    Matrix.setRotateM(rotationMatrix, 0, butterfly_angle, 0.0f, -1.0f, 0.0f);
                    Matrix.multiplyMM(modelViewMatrix_butterfly, 0, modelViewMatrix_butterfly, 0, rotationMatrix, 0);
                    Matrix.setIdentityM(translationMatrix, 0);
                    Matrix.translateM(translationMatrix, 0, butterfly_distance, 0.0f, 0.0f);
                    Matrix.multiplyMM(modelViewMatrix_butterfly, 0, modelViewMatrix_butterfly, 0, translationMatrix, 0);
                }

                Matrix.scaleM(modelViewMatrix_butterfly, 0, 0.08f, 0.08f, 0.08f);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_butterfly, 0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[5].get(0));
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[5].get(0));
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 6);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[5].getNumObjectVertex(0));
                Matrix.invertM(modelViewMatrix_butterfly_inverse, 0, modelViewMatrix_butterfly, 0);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_butterfly_inverse, 0);

                // fairy
                Matrix.setIdentityM(translationMatrix, 0);
                Matrix.translateM(translationMatrix, 0, xmove, 1.0f, zmove);
                Matrix.multiplyMM(modelViewMatrix_fairy, 0, modelViewMatrix, 0, translationMatrix, 0);

                Matrix.setIdentityM(rotationMatrix, 0);
                Matrix.setIdentityM(translationMatrix, 0);
                Matrix.setRotateM(rotationMatrix, 0, fairy_angle, 0.0f, -1.0f, 0.0f);
                Matrix.multiplyMM(modelViewMatrix_fairy, 0, modelViewMatrix_fairy, 0, rotationMatrix, 0);
                Matrix.translateM(translationMatrix, 0, fairy_distance, 0.0f, 0.0f);
                Matrix.multiplyMM(modelViewMatrix_fairy, 0, modelViewMatrix_fairy, 0, translationMatrix, 0);

                Matrix.scaleM(modelViewMatrix_fairy, 0, 4.0f, 4.0f, 4.0f);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_fairy, 0);
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false, 0, (FloatBuffer) verticeBuffers[6].get(0));
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, (FloatBuffer) textureBuffers[6].get(0));
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 7);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj[6].getNumObjectVertex(0));
                Matrix.invertM(modelViewMatrix_fairy_inverse, 0, modelViewMatrix_fairy, 0);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, projectionMatrix, 0, modelViewMatrix_fairy_inverse, 0);

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                SampleUtils.checkGLError("VirtualButtons renderFrame");


                if(state_crash_tree) {
                    for(int i=0;i<apple_height.length;i++) {
                        apple_height[i]-=0.1f;
                        if(apple_height[i]<0.0f)  {
                            apple_height[i]=0.0f;
                        }
                    }
                    state_crash_tree=false;
                }

                if(state_crash_tree2) {
                    for(int i=0;i<banana_height.length;i++) {
                    banana_height[i]-=0.1f;
                    if(banana_height[i]<0.0f) {
                        banana_height[i]=0.0f;
                    }
                    }
                    state_crash_tree2=false;
                }

                if(state_fruit_reset) {
                    apple_height[0]=5.0f;
                    apple_height[1]=5.0f;
                    apple_height[2]=4.0f;
                    banana_height[0]=5.0f;
                    banana_height[1]=5.5f;
                    xmove=0.0f; zmove=0.0f;
                    state_fruit_reset=false;
                }

                if(xmove>7) xmove=-7;
                if(xmove<-7) xmove=7;
                if(zmove>5) zmove=-5;
                if(zmove<-5) zmove=5;

                fairy_angle+=5.0f;
                if(fairy_angle>360.0f) fairy_angle=0.0f;

                butterfly_angle+=5.0f;
                if(butterfly_angle>360.0f)  {
                    state_butterfly = !state_butterfly;
                    butterfly_angle = 0.0f;
                }

                if(foxposition_x>1.5f && foxposition_x<4.5f && foxposition_z>-3.5f && foxposition_z<-0.5f) {
                    state_crash_tree=true;
                } // tree와 충돌
                if(foxposition_x>-3.5f && foxposition_x<-0.5f && foxposition_z>-3.5f && foxposition_z<-0.5f) {
                    state_crash_tree2=true;
                } // tree2와 충돌


            }

            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            Renderer.getInstance().end();
    }


    private Buffer fillBuffer(float[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        // Each float takes four bytes
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();

        return bb;

    }


    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;
    }
}
