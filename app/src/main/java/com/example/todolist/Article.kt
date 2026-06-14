package com.example.todolist

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val category: String, // e.g., "gym", "study", "gaming"
    val icon: String // Emoji or icon resource name
) : Parcelable

object ArticleProvider {
    val articles = listOf(
        Article(
            "gym_1",
            "Gym Guide for Beginners",
            "Starting your fitness journey? Here are the top 5 tips to stay consistent.",
            "1. Start Slow: Don't push too hard in the first week.\n2. Form over Weight: Focus on correct technique.\n3. Stay Hydrated: Drink plenty of water.\n4. Recovery is Key: Get at least 7-8 hours of sleep.\n5. Nutrition: Eat enough protein to help muscle recovery.",
            "gym",
            "🏋️"
        ),
        Article(
            "study_1",
            "Mastering Focus while Studying",
            "How to use the Pomodoro technique to double your productivity.",
            "1. Choose a Task: Pick one subject to focus on.\n2. Set a Timer: Work for 25 minutes.\n3. Take a Break: Rest for 5 minutes.\n4. Repeat: After 4 cycles, take a longer break.\n5. Environment: Keep your desk clean and phone away.",
            "study",
            "📚"
        ),
        Article(
            "gaming_1",
            "Balanced Gaming Lifestyle",
            "Enjoy your favorite games while staying healthy and productive.",
            "1. Eye Care: Follow the 20-20-20 rule.\n2. Posture: Sit straight to avoid back pain.\n3. Schedule: Set a specific time for gaming.\n4. Socialize: Play with friends to make it a social activity.\n5. Sleep: Don't let late-night sessions ruin your routine.",
            "gaming",
            "🎮"
        ),
        Article(
            "coding_1",
            "The Programmer's Mindset",
            "Tips for staying sharp while coding for long hours.",
            "1. Break it Down: Small problems are easier to solve.\n2. Rubber Ducking: Explain your code out loud.\n3. Version Control: Commit often, even small changes.\n4. Debugging: Take a walk when you're stuck.\n5. Learning: Read documentation before jumping into tutorials.",
            "coding",
            "💻"
        ),
        Article(
            "food_1",
            "Healthy Eating Habits",
            "Fuel your body for maximum energy throughout the day.",
            "1. Breakfast: Never skip it; it jumpstarts your metabolism.\n2. Portion Control: Use smaller plates to avoid overeating.\n3. Greens: Include vegetables in every meal.\n4. Sugars: Limit processed sugar intake.\n5. Mindful Eating: Don't watch TV while eating.",
            "food",
            "🍎"
        ),
        Article(
            "meditation_1",
            "Mental Clarity & Peace",
            "Quick mindfulness techniques to reduce stress and anxiety.",
            "1. Deep Breathing: Inhale for 4s, hold for 4s, exhale for 8s.\n2. Body Scan: Notice tension in each muscle group and release it.\n3. Nature: Spend 10 minutes outdoors without your phone.\n4. Gratitude: Write down 3 things you're thankful for today.\n5. Presence: Focus entirely on your current task.",
            "mental",
            "🧘"
        ),
        Article(
            "finance_1",
            "Money Management 101",
            "Smart ways to track your expenses and build savings.",
            "1. Budgeting: Use the 50/30/20 rule (Needs/Wants/Savings).\n2. Emergency Fund: Aim for 3-6 months of expenses.\n3. Track Everything: No expense is too small to record.\n4. Invest: Start early to benefit from compound interest.\n5. Debt: Prioritize paying off high-interest loans.",
            "finance",
            "💰"
        ),
        Article(
            "cleaning_1",
            "A Clean Space, A Clean Mind",
            "Tips for maintaining an organized home and workspace.",
            "1. 5-Minute Rule: If it takes less than 5 mins, do it now.\n2. Declutter: If you haven't used it in a year, donate it.\n3. Consistency: Set a 15-minute timer for daily tidying.\n4. Vertical Space: Use shelves to keep surfaces clear.\n5. Laundry: Fold clothes immediately after drying.",
            "home",
            "🧹"
        ),
        Article(
            "social_1",
            "Building Meaningful Connections",
            "How to improve your social skills and deepen relationships.",
            "1. Active Listening: Pay full attention when others speak.\n2. Vulnerability: Share your true thoughts and feelings.\n3. Quality Time: Put away devices when spending time with loved ones.\n4. Networking: Reach out to someone in your field for coffee.\n5. Support: Be there for others during their tough times.",
            "social",
            "🤝"
        ),
        Article(
            "sleep_1",
            "The Ultimate Sleep Routine",
            "Improve your sleep quality for better energy and mood.",
            "1. Consistency: Go to bed and wake up at the same time.\n2. No Screens: Avoid blue light 1 hour before sleep.\n3. Temperature: Keep your bedroom cool and dark.\n4. Caffeine: Stop consuming caffeine 8-10 hours before bed.\n5. Wind Down: Read a physical book or listen to calm music.",
            "sleep",
            "🌙"
        ),
        Article(
            "productivity_1",
            "High Performance Work Habits",
            "Get more done in less time with these proven strategies.",
            "1. Eat the Frog: Do your hardest task first thing in the morning.\n2. Batching: Group similar tasks together to avoid context switching.\n3. Deep Work: Block out time for distraction-free work sessions.\n4. Say No: Don't take on more than you can handle.\n5. Review: End your day by planning for the next one.",
            "work",
            "🚀"
        ),
        Article(
            "hobby_1",
            "Nurturing Your Creativity",
            "Keep your creative sparks flying with these simple tips.",
            "1. Explore: Try one new hobby every few months.\n2. Practice: Dedicate at least 30 minutes a day to your craft.\n3. Community: Join a group of like-minded creators.\n4. Feedback: Share your work and be open to suggestions.\n5. Fun: Remember why you started; keep it enjoyable.",
            "hobby",
            "🎨"
        ),
        Article(
            "travel_1",
            "Travel Like a Pro",
            "Make your journeys smooth, safe, and memorable.",
            "1. Packing: Carry only the essentials to travel light.\n2. Research: Learn a few basic phrases of the local language.\n3. Safety: Keep copies of your important documents.\n4. Culture: Be respectful of local customs and traditions.\n5. Moments: Put down the camera once in a while to enjoy the view.",
            "travel",
            "✈️"
        ),
        Article(
            "grooming_1",
            "Daily Self-Care & Grooming",
            "Look and feel your best every single day.",
            "1. Skincare: Cleanse, moisturize, and use sunscreen.\n2. Hair: Get regular trims and use the right products.\n3. Posture: Stand tall; it instantly boosts your confidence.\n4. Wardrobe: Wear clothes that fit well and make you feel good.\n5. Smile: It's the best accessory you can wear.",
            "grooming",
            "✨"
        ),
        Article(
            "music_1",
            "The Joy of Music",
            "Playing an instrument or singing can significantly improve your brain health.",
            "1. Consistency: Practice daily, even if it's just for 15 minutes.\n2. Ear Training: Try to play songs by ear to improve your musicality.\n3. Theory: Understanding the basics of rhythm and melody makes learning easier.\n4. Recording: Record yourself to track progress and identify areas for improvement.\n5. Variety: Explore different genres to keep your passion alive.",
            "music",
            "🎸"
        ),
        Article(
            "art_1",
            "Expressing Your Inner Artist",
            "Drawing and painting are great ways to de-stress and express yourself.",
            "1. Fundamentals: Focus on shapes, lines, and shading first.\n2. Inspiration: Look at other artists' work, but find your own style.\n3. Quality Tools: You don't need the most expensive gear, but good basics help.\n4. Sketch Daily: Carry a small sketchbook to capture ideas on the go.\n5. No Perfectionism: Allow yourself to make mistakes and learn from them.",
            "art",
            "🖌️"
        ),
        Article(
            "writing_1",
            "Becoming a Better Writer",
            "Writing is a skill that improves with constant practice and reading.",
            "1. Daily Habit: Write at least 200 words every day.\n2. Read Widely: Reading different styles improves your own vocabulary.\n3. Editing: Your first draft is just for you; the magic happens in editing.\n4. Voice: Write like you speak to find your unique narrative voice.\n5. Structure: Use outlines to organize your thoughts before starting.",
            "writing",
            "✍️"
        ),
        Article(
            "tech_1",
            "Tech & Digital Wellness",
            "Stay tech-savvy while maintaining a healthy relationship with your devices.",
            "1. Security: Use strong, unique passwords and enable 2FA.\n2. Updates: Keep your software updated to protect against vulnerabilities.\n3. Backups: Regularly back up your important files to the cloud or a hard drive.\n4. Screen Time: Use apps to monitor and limit your digital consumption.\n5. Organization: Keep your desktop and files organized to reduce digital stress.",
            "tech",
            "🤖"
        ),
        Article(
            "nature_1",
            "Reconnecting with Nature",
            "Spending time outdoors is essential for physical and mental well-being.",
            "1. Outdoor Time: Try to spend at least 30 minutes outside every day.\n2. Gardening: Growing your own plants is a rewarding and calming hobby.\n3. Hiking: Explore local trails to get exercise and fresh air.\n4. No Devices: Leave your phone behind or on silent to fully immerse yourself.\n5. Respect: Always leave nature as you found it.",
            "nature",
            "🌲"
        ),
        Article(
            "language_1",
            "Language Learning Secrets",
            "Learn a new language faster with these immersion techniques.",
            "1. Immersion: Change your phone's language and watch movies in that language.\n2. Flashcards: Use Spaced Repetition Systems (SRS) like Anki for vocabulary.\n3. Speak Early: Don't wait until you're perfect to start speaking.\n4. Consistency: 15 minutes a day is better than 2 hours once a week.\n5. Fun: Learn through topics you already enjoy, like cooking or gaming.",
            "language",
            "🌐"
        ),
        Article(
            "reading_1",
            "Building a Life-Long Reading Habit",
            "Reading books opens up new worlds and expands your knowledge.",
            "1. Carry a Book: Always have something to read during spare moments.\n2. Set Goals: Aim to read a certain number of pages or chapters a day.\n3. Variety: Read both fiction and non-fiction to keep things interesting.\n4. Audiobooks: Great for 'reading' while commuting or doing chores.\n5. Reflection: Take notes or discuss books with friends to remember more.",
            "reading",
            "📖"
        ),
        Article(
            "cooking_1",
            "Mastering the Kitchen",
            "Cooking your own meals is healthier, cheaper, and more satisfying.",
            "1. Knife Skills: Learning how to use a knife properly makes cooking faster and safer.\n2. Seasoning: Don't be afraid to use salt, pepper, and spices to enhance flavor.\n3. Preparation: Read the whole recipe and prep ingredients before you start cooking.\n4. Cleaning: Clean as you go to avoid a huge mess at the end.\n5. Experiment: Once you know the basics, start making recipes your own.",
            "food",
            "👨‍🍳"
        ),
        Article(
            "parenting_1",
            "Positive Parenting Tips",
            "Build a strong and healthy relationship with your children.",
            "1. Quality Time: Dedicate 15-20 minutes of undivided attention daily.\n2. Listening: Really listen to what your child is saying without judging.\n3. Routine: Children thrive on predictable schedules and clear boundaries.\n4. Encouragement: Focus on effort and progress rather than just results.\n5. Self-Care: You can't pour from an empty cup; take care of yourself too.",
            "social",
            "👪"
        ),
        Article(
            "safety_1",
            "Home & Personal Safety",
            "Simple steps to keep yourself and your living space secure.",
            "1. Home Security: Ensure all locks are sturdy and windows are secure.\n2. Emergency Plan: Know what to do in case of fire or other emergencies.\n3. Awareness: Stay aware of your surroundings when out in public.\n4. First Aid: Keep a well-stocked first aid kit and learn basic CPR.\n5. Fire Safety: Test smoke detectors monthly and have a fire extinguisher ready.",
            "home",
            "🛡️"
        )
    )

    fun getArticleByCategory(category: String): Article? {
        return articles.find { it.category.lowercase() == category.lowercase() }
    }
}
