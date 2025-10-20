import utest.*

import clip.util.text.jaroSimilarity

object JaroWinklerTest extends TestSuite:

  val tests = Tests {
    test("jaro") {
      assert(jaroSimilarity("MARTHA", "MARHTA") == 0.9444444444444445)
      assert(jaroSimilarity("DIXON", "DICKSONS") == 0.7666666666666666)
      assert(jaroSimilarity("JELLYFISH", "SMELLYFISH") == 0.8962962962962964)
    }
    test("jaro-winkler") {
      assert(jaroSimilarity("DWAYNE", "DUANE") == 0.8222222222222223)
      assert(jaroSimilarity("MARTHA", "MARHTA") == 0.9444444444444445)
      assert(jaroSimilarity("DIXON", "DICKSONX") == 0.7666666666666666)
      assert(jaroSimilarity("JELLYFISH", "SMELLYFISH") == 0.8962962962962964)
    }
  }
