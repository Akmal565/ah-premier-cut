package com.vfx.engine.effects

import com.vfx.engine.core.effect.WorkingSpace
import com.vfx.engine.core.effect.EffectCategory
import com.vfx.engine.core.effect.EffectDefinition
import com.vfx.engine.core.effect.EffectSnapshot
import com.vfx.engine.core.params.FloatP
import com.vfx.engine.core.params.IntP
import com.vfx.engine.core.params.Vec2P
import com.vfx.engine.gpu.GpuTexture
import com.vfx.engine.gpu.glsl.EffectGlsl
import com.vfx.engine.graph.GraphBuilder
import kotlin.math.exp

/** Separable H->V blur shared by gaussian/box/glow/unsharp. */
class SeparableBlurRuntime(private val box: Boolean) : MultiPassRuntimeBase() {
    override val workingSpace = WorkingSpace.GAMMA

    override fun build(b: GraphBuilder, input: GpuTexture, s: EffectSnapshot): GpuTexture {
        val ctx = b.renderCtx
        val radius = (s.float("radius") * ctx.quality.blurScale).toInt().coerceIn(1, 16)
        val sigma = s.float("sigma").coerceAtLeast(0.1f)
        val weights = if (box) boxWeights(radius) else gaussianWeights(radius, sigma)
        val p = program(ctx, if (box) "blur.box" else "blur.gaussian", EffectGlsl.SEPARABLE_BLUR)
        // u_dir is the PER-TAP STEP. The tap count already spans +-radius (taps = 2r+1), so the
        // step must be exactly one texel. Multiplying by radius again spread the kernel over
        // radius^2 texels (radius 16 -> +-256 px sampled every 16th texel: severe comb/banding
        // artifacts instead of a blur).
        val h = b.addPass("blur.h", p, input, outWidth = outW(ctx), outHeight = outH(ctx)) { prog ->
            prog.setFloatArray("u_weights", weights)
            prog.setInt("u_taps", weights.size)
            prog.setVec2("u_dir", 1f / input.width.toFloat(), 0f)
            setIntensityOne(prog)
        }
        return b.addPass("blur.v", p, h, outWidth = outW(ctx), outHeight = outH(ctx)) { prog ->
            prog.setFloatArray("u_weights", weights)
            prog.setInt("u_taps", weights.size)
            prog.setVec2("u_dir", 0f, 1f / h.height.toFloat())
            setIntensity(prog, s)   // mix once, on the final pass
        }
    }

    /**
     * Output size for both passes. Null (default) renders at the frame size; bloom passes a
     * half-res target so its blur does not silently run at full resolution after the half-res
     * threshold pass (defeating the documented downsample and doubling fill-rate).
     */
    var outputSize: Pair<Int, Int>? = null

    private fun outW(ctx: com.vfx.engine.graph.RenderContext): Int =
        outputSize?.first ?: ctx.frameWidth
    private fun outH(ctx: com.vfx.engine.graph.RenderContext): Int =
        outputSize?.second ?: ctx.frameHeight

    private fun gaussianWeights(radius: Int, sigma: Float): FloatArray {
        val n = radius * 2 + 1
        val w = FloatArray(n)
        var sum = 0f
        for (i in 0 until n) {
            val x = (i - radius).toFloat()
            w[i] = exp(-(x * x) / (2f * sigma * sigma))
            sum += w[i]
        }
        for (i in 0 until n) w[i] /= sum
        return w
    }

    private fun boxWeights(radius: Int): FloatArray {
        val n = radius * 2 + 1
        return FloatArray(n) { 1f / n }
    }
}

object BlurEffects {

    fun all(): List<EffectDefinition> = listOf(
        EffectDefinition("blur.gaussian", "Gaussian Blur", EffectCategory.BLUR,
            listOf(FloatP("radius", "Radius", 0f, 16f, 0.5f, 4f),
                   FloatP("sigma", "Sigma", 0.1f, 8f, 0.1f, 2f)),
            description = "Separable H->V, up to 33 taps. Passes: 2.")
        { SeparableBlurRuntime(box = false) },

        EffectDefinition("blur.box", "Box Blur", EffectCategory.BLUR,
            listOf(FloatP("radius", "Radius", 0f, 16f, 0.5f, 4f),
                   FloatP("sigma", "(unused)", 0.1f, 8f, 0.1f, 1f)),
            description = "Uniform-weight separable blur. Passes: 2.")
        { SeparableBlurRuntime(box = true) },

        simpleEffect("blur.directional", "Directional Blur", EffectCategory.BLUR,
            listOf(Vec2P("dir", "Direction", floatArrayOf(1f, 0f)),
                   FloatP("distance", "Distance", 0f, 0.3f, 0.005f, 0.02f),
                   IntP("samples", "Samples", 2, 64, 24)), EffectGlsl.DIRECTIONAL_BLUR,
            description = "Streak along a direction vector; doubles as motion blur.")
        { p, s, _ ->
            val d = s.floats("dir")
            p.setVec2("u_dir", d[0], d[1])
            p.setInt("u_samples", s.int("samples"))
            p.setFloat("u_amount", s.float("distance")) },

        simpleEffect("blur.radial", "Radial Blur", EffectCategory.BLUR,
            listOf(FloatP("cx", "Center X", 0f, 1f, 0.01f, 0.5f),
                   FloatP("cy", "Center Y", 0f, 1f, 0.01f, 0.5f),
                   FloatP("strength", "Strength", 0f, 0.5f, 0.005f, 0.05f),
                   IntP("samples", "Samples", 2, 64, 24)), EffectGlsl.RADIAL_BLUR)
        { p, s, _ ->
            p.setVec2("u_center", s.float("cx"), s.float("cy"))
            p.setFloat("u_strength", s.float("strength"))
            p.setInt("u_samples", s.int("samples")) },

        simpleEffect("blur.zoom", "Zoom Blur", EffectCategory.BLUR,
            listOf(FloatP("cx", "Center X", 0f, 1f, 0.01f, 0.5f),
                   FloatP("cy", "Center Y", 0f, 1f, 0.01f, 0.5f),
                   FloatP("strength", "Strength", 0f, 0.5f, 0.005f, 0.1f),
                   IntP("samples", "Samples", 2, 64, 32)), EffectGlsl.ZOOM_BLUR)
        { p, s, _ ->
            p.setVec2("u_center", s.float("cx"), s.float("cy"))
            p.setFloat("u_strength", s.float("strength"))
            p.setInt("u_samples", s.int("samples")) },

        simpleEffect("sharpen.sharpen", "Sharpen", EffectCategory.SHARPEN,
            listOf(FloatP("amount", "Amount", 0f, 2f, 0.01f, 0.4f)), EffectGlsl.SHARPEN_3X3)
        { p, s, _ -> p.setFloat("u_amount", s.float("amount")) },

        EffectDefinition("sharpen.unsharp", "Unsharp Mask", EffectCategory.SHARPEN,
            listOf(FloatP("amount", "Amount", 0f, 3f, 0.01f, 1f),
                   FloatP("radius", "Radius", 0f, 16f, 0.5f, 3f),
                   FloatP("sigma", "Sigma", 0.1f, 8f, 0.1f, 1.5f)),
            description = "Blur (2 passes) + detail re-add. Passes: 3.")
        { UnsharpRuntime() }
    )

    private class UnsharpRuntime : MultiPassRuntimeBase() {
        override val workingSpace = WorkingSpace.GAMMA
        override fun build(b: GraphBuilder, input: GpuTexture, s: EffectSnapshot): GpuTexture {
            val blurred = SeparableBlurRuntime(box = false).build(b, input,
                EffectSnapshot("unsharp.blur", s.timeUs,
                    mapOf("radius" to s.float("radius"), "sigma" to s.float("sigma")), 1f))
            val p = program(b.renderCtx, "unsharp.combine", EffectGlsl.UNSHARP_COMBINE)
            // The combine pass binds u_blurred directly in its lambda below; a pre-pass
            // addSecondaryInput would only patch the blur.v pass (stray sampler binding).
            return b.addPass("unsharp.combine", p, input) { prog ->
                prog.setTexture("u_blurred", blurred)
                prog.setFloat("u_amount", s.float("amount"))
                setIntensity(prog, s)
            }
        }
    }
}
