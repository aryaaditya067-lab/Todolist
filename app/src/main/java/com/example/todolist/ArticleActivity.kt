package com.example.todolist

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView

class ArticleActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        val category = intent.getStringExtra("CATEGORY")
        val article = if (category != null) {
            ArticleProvider.getArticleByCategory(category)
        } else {
            intent.getParcelableExtra<Article>("EXTRA_ARTICLE")
        }

        if (article == null) {
            finish()
            return
        }

        findViewById<TextView>(R.id.txtArticleIcon).text = article.icon
        findViewById<TextView>(R.id.txtArticleTitle).text = article.title
        findViewById<TextView>(R.id.txtArticleContent).text = article.content
        findViewById<TextView>(R.id.txtToolbarTitle).text = "${article.category.replaceFirstChar { it.uppercase() }} Tips"

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGotIt).setOnClickListener { finish() }
    }

    override fun applySettings() {
        super.applySettings()
        findViewById<Button>(R.id.btnGotIt)?.backgroundTintList = android.content.res.ColorStateList.valueOf(settingsManager.accentColor)
    }
}
