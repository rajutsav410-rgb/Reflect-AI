package com.example.data.model

enum class ReflectionMode(
    val title: String,
    val description: String,
    val promptPrefix: String,
    val iconName: String
) {
    REFLECTION(
        title = "Reflection",
        description = "Deep insights, mindfulness & psychological perspective",
        promptPrefix = "Analyze my journal reflection with deep psychological insights, compassionate perspective, and mindful growth points.",
        iconName = "Psychology"
    ),
    SUMMARIZATION(
        title = "Summarize",
        description = "Distill key takeaways, thoughts, and core highlights",
        promptPrefix = "Provide a concise, bulleted executive summary and core emotional/strategic takeaways of my entry.",
        iconName = "Summarize"
    ),
    BRAINSTORMING(
        title = "Brainstorm",
        description = "Generate creative solutions, alternative angles & ideas",
        promptPrefix = "Brainstorm creative possibilities, alternative paths, and out-of-the-box ideas based on what I wrote.",
        iconName = "Lightbulb"
    ),
    ACTION_PLAN(
        title = "Action Plan",
        description = "Step-by-step roadmap and practical next actions",
        promptPrefix = "Create a structured, prioritize-driven action plan with concrete milestones and immediate next steps.",
        iconName = "Checklist"
    ),
    DEEP_INQUIRY(
        title = "Inquiry",
        description = "Socratic questions to unlock deeper clarity",
        promptPrefix = "Act as a thoughtful Socratic mentor. Pose 3-4 powerful exploratory questions to help me examine my assumptions deeper.",
        iconName = "HelpOutline"
    )
}
