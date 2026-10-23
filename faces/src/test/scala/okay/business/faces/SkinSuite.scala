package okay.business.faces

import okay.faces.{Color, Look}

/** okay-ui's look: the default is the calm skin; a skin's colours and scheme are in its stylesheet and its look */
class SkinSuite extends munit.FunSuite:
  test("the default is calm, in its own colours, light or dark by the system"):
    assertEquals(Skin.default, Skin.calm())
    val css = Skin.default.css
    assert(css.contains("--ui-accent: #3b5bdb"), css)
    assert(css.contains("@media (prefers-color-scheme: dark)"), css)
    assert(css.contains("body .okay-form {"), css)

  test("an application's accent: in the stylesheet and in the look okay-faces reads"):
    val skin = Skin.calm(accent = "#0B7A5C", deep = "#0d3b30")
    assert(skin.css.contains("--ui-accent: #0b7a5c; --ui-deep: #0d3b30"), skin.css)
    assertEquals(skin.look.theme.palette.accent, Color("#0b7a5c"))

  test("a scheme said is the one always"):
    for scheme <- List(Scheme.Light, Scheme.Dark) do
      assert(!Skin.calm(scheme = scheme).css.contains("prefers-color-scheme"), scheme)
    assert(Skin.calm(scheme = Scheme.Dark).css.contains("--okay-bg: #12141b"))
    assert(Skin.calm(scheme = Scheme.Light).css.contains("--okay-bg: #f4f5f9"))

  test("plain is okay-faces' look alone"):
    assertEquals(Skin.plain, Skin(Look.default, ""))

  test("a colour that is not one is refused when the skin is made"):
    intercept[RuntimeException](Skin.calm(accent = "green"))
