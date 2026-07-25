package com.yourname.vf.media

import android.opengl.GLES20

class V360Shader {
    var program = 0
        private set

    private val vertexShaderCode = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        uniform vec2 uInputSize;
        uniform vec2 uOutputSize;
        uniform float uYaw, uPitch, uRoll, uFov;

        const float PI = 3.14159265359;

        vec3 rotate(vec3 dir, float yaw, float pitch, float roll) {
            float cosY = cos(yaw * PI / 180.0);
            float sinY = sin(yaw * PI / 180.0);
            dir = vec3(dir.x * cosY + dir.z * sinY, dir.y, -dir.x * sinY + dir.z * cosY);
            float cosP = cos(pitch * PI / 180.0);
            float sinP = sin(pitch * PI / 180.0);
            dir = vec3(dir.x, dir.y * cosP - dir.z * sinP, dir.y * sinP + dir.z * cosP);
            return dir;
        }

        void main() {
            float f = 1.0 / tan(uFov * PI / 360.0);
            vec2 ndc = (vTexCoord - 0.5) * 2.0;
            vec3 dir = normalize(vec3(ndc.x, ndc.y, f));
            dir = rotate(dir, uYaw, uPitch, uRoll);
            float lat = asin(dir.y);
            float lon = atan(dir.z, dir.x);
            vec2 equiUV = vec2(lon / (2.0 * PI) + 0.5, 0.5 - lat / PI);
            gl_FragColor = texture2D(uTexture, equiUV);
        }
    """.trimIndent()

    private var aPosition = 0
    private var aTexCoord = 0
    private var uTexture = 0
    private var uInputSize = 0
    private var uOutputSize = 0
    private var uYaw = 0
    private var uPitch = 0
    private var uRoll = 0
    private var uFov = 0

    fun build() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTexture = GLES20.glGetUniformLocation(program, "uTexture")
        uInputSize = GLES20.glGetUniformLocation(program, "uInputSize")
        uOutputSize = GLES20.glGetUniformLocation(program, "uOutputSize")
        uYaw = GLES20.glGetUniformLocation(program, "uYaw")
        uPitch = GLES20.glGetUniformLocation(program, "uPitch")
        uRoll = GLES20.glGetUniformLocation(program, "uRoll")
        uFov = GLES20.glGetUniformLocation(program, "uFov")
    }

    fun use() = GLES20.glUseProgram(program)

    fun setSize(inputW: Int, inputH: Int, outputW: Int, outputH: Int) {
        GLES20.glUniform2f(uInputSize, inputW.toFloat(), inputH.toFloat())
        GLES20.glUniform2f(uOutputSize, outputW.toFloat(), outputH.toFloat())
    }

    fun setRotation(yaw: Float, pitch: Float, roll: Float) {
        GLES20.glUniform1f(uYaw, yaw)
        GLES20.glUniform1f(uPitch, pitch)
        GLES20.glUniform1f(uRoll, roll)
    }

    fun setFov(fov: Float) {
        GLES20.glUniform1f(uFov, fov)
    }

    fun bindTexture(texId: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glUniform1i(uTexture, 0)
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }
}
