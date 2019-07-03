package com.picozense.sdk.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "MyRenderer";
    private int program;
    private Context context;
    Bitmap bitmap;
    int textureId;

    Bitmap bufYuv;
    int mPositionHandle = 0;
    int mTexCoordHandle = 0;
    int mTexSamplerHandle = 0;
    //CameraPreview m_cameraPreview;

    float vertices[] = {
            1.0f, 1.0f, 0.0f,   // top right
            -1.0f, 1.0f, 0.0f,  // top left
            -1.0f, -1.0f, 0.0f, // bottom left
            1.0f, -1.0f, 0.0f,  // bottom right

    };

    private static final float[] TEX_VERTEX = {   // in clockwise order:
            1.0f, 0.0f,  // bottom right
            0.0f, 0.0f,  // bottom left
            0.0f, 1.0f,  // top left
            1.0f, 1.0f,  // top right
    };
    private static final short[] VERTEX_INDEX = { 0, 1, 2, 0, 2, 3 };

    ShortBuffer mVertexIndexBuffer;
    FloatBuffer vertexBuf;
    FloatBuffer mTexVertexBuffer;
    
    public void setBuf(Bitmap  bitmap){
    	bufYuv = bitmap;
    }
    public MyRenderer(Context context)
    {
        this.context=context;
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuf = vbb.asFloatBuffer();
        vertexBuf.put(vertices);
        vertexBuf.position(0);

        mTexVertexBuffer = ByteBuffer.allocateDirect(TEX_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEX_VERTEX);
        mTexVertexBuffer.position(0);

        mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(VERTEX_INDEX);
        mVertexIndexBuffer.position(0);
    }

    private int loadShader(int shaderType,String sourceCode) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, sourceCode);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("ES20_ERROR", "Could not compile shader " + shaderType + ":");
                Log.e("ES20_ERROR", GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, pixelShader);
            GLES20.glLinkProgram(program);

            GLES20.glUseProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e("ES20_ERROR", "Could not link program: ");
                Log.e("ES20_ERROR", GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        program = createProgram(verticesShader, fragmentShader);
        GLES20.glClearColor(0.0f, 0, 0, 0.0f);

        int[] texNames = new int[1];
        GLES20.glGenTextures(1, texNames, 0);
        int mTexName = texNames[0];
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexName);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);
        //GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, m_cameraPreview.GetBitmap(), 0);
        //bitmap.recycle();

        mPositionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        mTexCoordHandle = GLES20.glGetAttribLocation(program, "a_texCoord");
        mTexSamplerHandle = GLES20.glGetUniformLocation(program, "s_texture");

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false,
                12, vertexBuf);

        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0,
                mTexVertexBuffer);

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0,0,width,height);
    }

    public void onDrawFrame(GL10 gl10) {
        GLES20.glUniform1i(mTexSamplerHandle, 0);
        if(bufYuv !=  null)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bufYuv, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length,
                GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);

    }

    private static final String verticesShader
            = "attribute vec4 vPosition;" +
            "attribute vec2 a_texCoord;" +
            "varying vec2 v_texCoord;" +
            "void main() {" +
            " gl_Position = vPosition;" +
            " v_texCoord = a_texCoord;" +
            "}";

    private static final String fragmentShader
            = "precision mediump float;" +
            "varying vec2 v_texCoord;" +
            "uniform sampler2D s_texture;" +
            "void main() {" +
            "vec4 nColor=texture2D(s_texture,v_texCoord);"+
            " gl_FragColor = vec4(nColor.r,nColor.g,nColor.b,nColor.a);" +
            "}";
}
