package com.almosoft.eyecare_202020rule

data class Exercise(
    val name: String,
    val emoji: String,
    val description: String,
    val durationSeconds: Int,
    val steps: List<String>
)

data class EyeTip(
    val category: String,
    val emoji: String,
    val title: String,
    val description: String
)

data class EyeFact(
    val text: String
)

val exercises = listOf(
    Exercise(
        "Palming", "👐", "Relieve eye strain and relax the nervous system.", 30,
        listOf("Rub palms together until warm.", "Cup them gently over closed eyes.", "Breathe deeply and relax.")
    ),
    Exercise(
        "Blinking", "👁️", "Moisturize your eyes and reduce dryness.", 10,
        listOf("Blink rapidly and consciously for 10 full seconds.")
    ),
    Exercise(
        "Figure 8", "♾️", "Increase eye muscle flexibility.", 30,
        listOf("Imagine a figure 8 on the wall.", "Trace it slowly with your eyes.", "Reverse direction after 15 seconds.")
    ),
    Exercise(
        "Near & Far Focus", "🎯", "Strengthen the focusing muscles.", 30,
        listOf("Hold thumb 10 inches from face.", "Focus on thumb for 5 seconds.", "Focus on object 20 feet away for 5 seconds.", "Repeat 3 times.")
    ),
    Exercise(
        "Up-Down Movement", "↕️", "Improve vertical eye tracking.", 20,
        listOf("Slowly look up as far as comfortable.", "Hold 2 seconds.", "Look down.", "Repeat 5 times.")
    ),
    Exercise(
        "Left-Right Movement", "↔️", "Improve horizontal eye tracking.", 20,
        listOf("Look left as far as comfortable.", "Hold 2 seconds.", "Look right.", "Repeat 5 times.")
    ),
    Exercise(
        "Diagonal Movement", "↗️", "Improve diagonal eye tracking.", 20,
        listOf("Look top-left to bottom-right.", "Hold 2 seconds.", "Reverse.", "Repeat 5 times.")
    ),
    Exercise(
        "Eye Rolling", "🔄", "Complete eye muscle workout.", 30,
        listOf("Roll eyes slowly clockwise 5 times.", "Then anticlockwise 5 times.")
    )
)

val eyeTips = listOf(
    EyeTip("Screen Settings", "🔅", "Reduce Brightness", "Reduce screen brightness in low light environments."),
    EyeTip("Screen Settings", "🌙", "Night Mode", "Enable Night Mode / Blue light filter to reduce eye strain."),
    EyeTip("Screen Settings", "🅰️", "Font Size", "Increase font size to reduce squinting and fatigue."),
    EyeTip("Screen Settings", "📏", "Distance", "Keep screen at arm's length distance (about 20-28 inches)."),
    EyeTip("Screen Settings", "👓", "Anti-glare", "Use matte screen protector to reduce glare."),
    
    EyeTip("Workspace Setup", "🖥️", "Monitor Position", "Monitor should be 20-28 inches from your eyes."),
    EyeTip("Workspace Setup", "📐", "Eye Level", "Top of screen should be at eye level or slightly below."),
    EyeTip("Workspace Setup", "🪟", "Avoid Glare", "Avoid working with a bright window directly behind the screen."),
    EyeTip("Workspace Setup", "💡", "Lighting", "Use indirect warm lighting in the room."),
    EyeTip("Workspace Setup", "🪑", "Ergonomics", "Adjust chair so feet are flat on the floor."),
    
    EyeTip("Daily Habits", "💧", "Hydration", "Drink 8 glasses of water daily for eye moisture."),
    EyeTip("Daily Habits", "😴", "Sleep", "Sleep 7-8 hours to rest your eyes properly."),
    EyeTip("Daily Habits", "🥕", "Nutrition", "Eat Vitamin A rich foods like carrots and spinach."),
    EyeTip("Daily Habits", "🕶️", "Protection", "Wear anti-glare glasses if prescribed by a doctor."),
    EyeTip("Daily Habits", "🚶", "Movement", "Take a 5-minute walk every hour to break screen time."),
    
    EyeTip("Warning Signs", "🤕", "Headaches", "Frequent headaches after screen use may indicate eye strain."),
    EyeTip("Warning Signs", "🌫️", "Blurred Vision", "Blurred or double vision should be checked by a professional."),
    EyeTip("Warning Signs", "🔴", "Dry/Red Eyes", "Dry, red, or itchy eyes lasting days need attention."),
    EyeTip("Warning Signs", "☀️", "Light Sensitivity", "Increased sensitivity to light can be a warning sign."),
    EyeTip("Warning Signs", "🔍", "Focus Difficulty", "Difficulty focusing on near or far objects requires an eye exam.")
)

val eyeFacts = listOf(
    EyeFact("The average person blinks 15-20 times per minute, but only 5-7 times when staring at a screen."),
    EyeFact("Blue light from screens can penetrate all the way to the retina."),
    EyeFact("Digital Eye Strain affects over 60% of people who use screens daily."),
    EyeFact("Eyes can get sunburned — called Photokeratitis."),
    EyeFact("The human eye can distinguish approximately 10 million different colors."),
    EyeFact("Eye muscles are the most active muscles in the human body."),
    EyeFact("Carrots help eye health due to beta-carotene which converts to Vitamin A."),
    EyeFact("Reading in dim light does not damage your eyes, but it does cause fatigue."),
    EyeFact("The 20-20-20 rule was introduced by optometrist Dr. Jeffrey Anshel."),
    EyeFact("Screen time before bed reduces melatonin by up to 50%, affecting sleep quality.")
)
