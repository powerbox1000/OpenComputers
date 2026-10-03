package li.cil.oc.integration

import dev.emi.emi.api.{EmiEntrypoint, EmiPlugin, EmiRegistry}
import dev.emi.emi.api.recipe.{EmiRecipe, EmiRecipeCategory}
import dev.emi.emi.api.stack.{EmiIngredient, EmiStack}
import li.cil.oc.{Localization, OpenComputers, api}
import li.cil.oc.common.ContentVisibility
import li.cil.oc.common.init.OCItems
import li.cil.oc.integration.jei.{CallbackDocHandler, ManualUsageHandler}
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import dev.emi.emi.api.widget.{Bounds, Widget, WidgetHolder}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

import scala.jdk.CollectionConverters._

@EmiEntrypoint
final class ModEMI extends EmiPlugin {
  override def register(registry: EmiRegistry): Unit = {
    registry.addCategory(ManualEmiRecipe.Category)
    registry.addCategory(CallbackDocEmiRecipe.Category)

    if (!ContentVisibility.hiddenItems.isEmpty) {
      registry.removeEmiStacks(stack => ContentVisibility.isHidden(stack.getItemStack))
    }

    BuiltInRegistries.ITEM.stream().iterator().asScala.foreach { item =>
      val stack = item.getDefaultInstance
      if (!stack.isEmpty && !ContentVisibility.isHidden(stack)) {
        ManualUsageHandler.recipeFor(stack).foreach(recipe => registry.addRecipe(new ManualEmiRecipe(recipe)))
        CallbackDocHandler.recipesFor(stack).foreach(recipe => registry.addRecipe(new CallbackDocEmiRecipe(recipe)))
      }
    }
  }
}

private object EmiCategoryIds {
  def apply(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, path)
}

private final class ManualEmiRecipe(recipe: ManualUsageHandler.ManualUsageRecipe) extends EmiRecipe {
  private val input = EmiStack.of(recipe.stack)

  override def getCategory: EmiRecipeCategory = ManualEmiRecipe.Category
  override def getId: ResourceLocation = EmiCategoryIds(s"/manual_usage/${BuiltInRegistries.ITEM.getKey(recipe.stack.getItem).getPath}")
  override def getInputs: java.util.List[EmiIngredient] = java.util.List.of(input)
  override def getOutputs: java.util.List[EmiStack] = java.util.List.of()
  override def getDisplayWidth: Int = 160
  override def getDisplayHeight: Int = 35
  override def supportsRecipeTree: Boolean = false

  override def addWidgets(widgets: WidgetHolder): Unit = {
    val bounds = new Bounds(28, 5, 100, 20)
    widgets.add(new Widget {
      override def getBounds: Bounds = bounds

      override def render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float): Unit = {
        val hovered = bounds.contains(mouseX, mouseY)
        val fill = if (hovered) 0xFFA0A0A0 else 0xFF707070
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), fill)
        graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1, 0xFF202020)
        graphics.drawCenteredString(Minecraft.getInstance.font, Localization.localizeLater("nei.usage.oc.Manual"),
          bounds.x() + bounds.width() / 2, bounds.y() + 6, 0xFFFFFF)
      }

      override def mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean = {
        if (button != 0 || !bounds.contains(mouseX, mouseY)) return false
        val minecraft = Minecraft.getInstance
        if (minecraft.player == null) return false
        minecraft.player.closeContainer()
        api.Manual.openFor(minecraft.player)
        api.Manual.navigate(recipe.path)
        true
      }
    })
  }
}

private object ManualEmiRecipe {
  val Category: EmiRecipeCategory = new EmiRecipeCategory(
    EmiCategoryIds("manual_usage"),
    EmiStack.of(OCItems.Manual.get())
  ) {
    override def getName: Component = Component.literal("OpenComputers Manual")
  }
}

private final class CallbackDocEmiRecipe(recipe: CallbackDocHandler.CallbackDocRecipe) extends EmiRecipe {
  private val input = EmiStack.of(recipe.stack)

  override def getCategory: EmiRecipeCategory = CallbackDocEmiRecipe.Category
  override def getId: ResourceLocation = ResourceLocation.fromNamespaceAndPath(
    OpenComputers.ID,
    s"/callback_doc/${BuiltInRegistries.ITEM.getKey(recipe.stack.getItem).getPath}/${Integer.toHexString(recipe.page.hashCode)}"
  )
  override def getInputs: java.util.List[EmiIngredient] = java.util.List.of(input)
  override def getOutputs: java.util.List[EmiStack] = java.util.List.of()
  override def getDisplayWidth: Int = 160
  override def getDisplayHeight: Int = 125
  override def supportsRecipeTree: Boolean = false

  override def addWidgets(widgets: WidgetHolder): Unit = {
    widgets.addDrawable(0, 0, getDisplayWidth, getDisplayHeight, (graphics, _, _, _) => {
      val font = net.minecraft.client.Minecraft.getInstance.font
      recipe.page.linesIterator.zipWithIndex.foreach { case (line, index) =>
        graphics.drawString(font, line, 4, 4 + index * (font.lineHeight + 1), 0x333333, false)
      }
    })
  }
}

private object CallbackDocEmiRecipe {
  val Category: EmiRecipeCategory = new EmiRecipeCategory(
    EmiCategoryIds("callback_doc"),
    EmiStack.of(OCItems.Tablet.get())
  ) {
    override def getName: Component = Component.literal("OpenComputers API")
  }
}
