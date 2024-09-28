package com.mlab.pran

import androidx.annotation.DrawableRes

enum class Website(val url: String,@DrawableRes val icon: Int = R.drawable.web) {
    Home("https://www.google.com.bd/", R.drawable.home),
    PIP_MISContactList("file:///android_asset/contact_list.html"),
    PIP_MIS("http://pipmis.pip.prangroup.com"),
    HRIS("https://hris.prangroup.com:777/Login.aspx"),
    Dispatch("http://hrms.prangroup.com:8283/ya/Login.aspx"),
    SmartManufacturing("http://prod.rflgroupbd.com:5000"),
    QC_PRAN_RFL("http://pqc.prangroup.com:8111/login"),
    FileShare("https://fs.prangroup.com"),
    FrozenFood("http://frozenfood.prangroup.com:7082"),
    PMC_eBill("http://pmc.prangroup.com/ebill/Login/Index")
    // Add more websites as needed
}


val Website.pageName
    get() =name
        .replace('_', ' ')
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace(Regex("([A-Z])([A-Z])([a-z])"), "$1 $2$3")

val String?.website
    get() = Website.entries.find { it.url == this } ?: Website.Home