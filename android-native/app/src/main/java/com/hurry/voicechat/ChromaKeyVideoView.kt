package com.hurry.voicechat

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Renders the emoji MP4 through a small GLES chroma-key shader.
 * Only saturated green pixels are keyed; playback remains ExoPlayer-driven.
 */
class ChromaKeyVideoView(
    context: Context,
    private val onVideoSurfaceReady: (Surface) -> Unit
) : GLSurfaceView(context) {
    private val videoRenderer = ChromaKeyRenderer(onVideoSurfaceReady)

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setRenderer(videoRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun release() {
        onPause()
        queueEvent { videoRenderer.release() }
    }
}

private class ChromaKeyRenderer(
    private val onSurfaceReady: (Surface) -> Unit
) : GLSurfaceView.Renderer {
    private var program = 0
    private var textureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var outputSurface: Surface? = null
    private val transform = FloatArray(16)
    private val vertices = floatBuffer(floatArrayOf(
        -1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f
    ))
    private val texCoords = floatBuffer(floatArrayOf(
        0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f
    ))

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val vertexShader = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            uniform mat4 uTexMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
            }
        """.trimIndent()
        val fragmentShader = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            varying vec2 vTexCoord;
            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                float greenExcess = color.g - max(color.r, color.b);
                float key = smoothstep(0.12, 0.30, greenExcess);
                color.a *= (1.0 - key);
                gl_FragColor = vec4(color.rgb * color.a, color.a);
            }
        """.trimIndent()

        program = linkProgram(vertexShader, fragmentShader)
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        surfaceTexture = SurfaceTexture(textureId).also { texture ->
            outputSurface = Surface(texture)
            onSurfaceReady(outputSurface!!)
        }
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val texture = surfaceTexture ?: return
        texture.updateTexImage()
        texture.getTransformMatrix(transform)

        GLES20.glUseProgram(program)
        val position = GLES20.glGetAttribLocation(program, "aPosition")
        val texCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        val matrix = GLES20.glGetUniformLocation(program, "uTexMatrix")
        val sampler = GLES20.glGetUniformLocation(program, "uTexture")

        vertices.position(0)
        GLES20.glEnableVertexAttribArray(position)
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, vertices)
        texCoords.position(0)
        GLES20.glEnableVertexAttribArray(texCoord)
        GLES20.glVertexAttribPointer(texCoord, 2, GLES20.GL_FLOAT, false, 0, texCoords)
        GLES20.glUniformMatrix4fv(matrix, 1, false, transform, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(sampler, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(position)
        GLES20.glDisableVertexAttribArray(texCoord)
    }

    fun release() {
        outputSurface?.release()
        outputSurface = null
        surfaceTexture?.release()
        surfaceTexture = null
        if (textureId != 0) GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        if (program != 0) GLES20.glDeleteProgram(program)
        textureId = 0
        program = 0
    }

    private fun linkProgram(vertex: String, fragment: String): Int {
        fun compile(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) { GLES20.glGetShaderInfoLog(shader) }
            return shader
        }
        val vs = compile(GLES20.GL_VERTEX_SHADER, vertex)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fragment)
        return GLES20.glCreateProgram().also { result ->
            GLES20.glAttachShader(result, vs)
            GLES20.glAttachShader(result, fs)
            GLES20.glLinkProgram(result)
            val status = IntArray(1)
            GLES20.glGetProgramiv(result, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) { GLES20.glGetProgramInfoLog(result) }
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
        }
    }

    private fun floatBuffer(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply { put(values); position(0) }
}
