package com.mlab.pran

enum class Website(val url: String) {
    HRIS("http://hrms.prangroup.com:8283/ya"),
    BanglalinkSMSCrop("http://smscorp.banglalinkgsm.com/smscorp/home.php?login=Y"),
    EducationBoardResults("http://www.educationboardresults.gov.bd/"),
    Google("https://www.google.com.bd/"),
    GoogleTranslate("https://translate.google.com.bd/?hl=bn"),
    ReversoSpellChecker("http://www.reverso.net/spell-checker/english-spelling-grammar/"),
    BengaliTyping("http://bengali.indiatyping.com/"),
    PrismProject("http://103.206.184.65:998/PRISM/Project/invlogin/index.php")
    // Add more websites as needed
}


val Website.pageName
    get() =name
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace(Regex("([A-Z])([A-Z])([a-z])"), "$1 $2$3")