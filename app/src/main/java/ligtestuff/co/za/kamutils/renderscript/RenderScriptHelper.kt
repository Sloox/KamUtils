package ligtestuff.co.za.kamutils.renderscript

import android.content.Context
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript

class RenderScriptHelper(ctx: Context, var allocation: Allocation) {
    val rs: RenderScript = RenderScript.create(ctx, RenderScript.ContextType.NORMAL)


}