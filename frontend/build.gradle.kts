plugins {
  kotlin("multiplatform")
}

kotlin {
  js(IR) {
    browser {

    }
    binaries.executable()
  }
}
