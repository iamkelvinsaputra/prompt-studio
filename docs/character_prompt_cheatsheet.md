# Modular Character Prompt System — Same Art Style, Plug-and-Play Components

Built from the original prompt’s style core: premium contemporary Japanese anime-game illustration, selective sumi-e brushwork, translucent sky-blue watercolor, restrained supernatural abstraction, strong negative space, and modern anime-game readability.【turn0file0†L3-L4】【turn0file0†L151-L159】【turn0file0†L165-L187】【turn0file0†L189-L212】

---

## 0. Goal

This file is a **modular prompt framework**.

Use it when you want:
- the **art style to stay constant**
- the **character, costume, pose, and other content components to be replaceable**
- a **plug-and-play structure** so you can swap components without rewriting the whole prompt every time

This is not meant to maximize prose. It is meant to maximize control.

---

## 1. Fixed Style Core (Do Not Change Often)

This is the **locked art-style block**. Keep this stable across different characters unless you intentionally want a new series style.

```text
ART STYLE CORE
A premium contemporary Japanese anime-game illustration with elegant, airy, slightly melancholic mood.
Combine sophisticated anime character rendering with selective Japanese sumi-e brushwork, translucent watercolor, restrained supernatural abstraction, and strong negative space.
Use expressive black ink brushwork selectively as a major graphic element, with visible pressure variation, thick-to-thin transitions, occasional dry-brush texture, imperfect endings, and controlled negative space.
Use soft sky-blue watercolor as the primary atmospheric color, luminous and translucent rather than neon, with subtle pigment pooling, soft bleed, and delicate overlap with black ink.
Maintain premium modern anime-game readability: believable adult proportions, controlled cel shading, restrained painterly softness, clean facial construction, clear costume construction, readable materials, and strong silhouette.
The final image should feel like polished flagship Japanese RPG key art with subtle tactile paper/washi texture embedded into the image rather than a literal blank sheet.
Avoid neon cyberpunk aesthetics, generic magic circles, random kanji, fake Japanese text, excessive particles, heavy ink splatter, cluttered backgrounds, exaggerated anatomy, or decorative AI noise.
```

### Style sub-components inside the locked core
These are the pieces you usually **keep fixed**:
1. **Rendering language** — anime-game illustration
2. **Ink language** — selective sumi-e brushwork
3. **Watercolor language** — soft sky-blue translucent wash
4. **Mood** — airy, elegant, quiet, slightly melancholic
5. **Graphic discipline** — negative space, asymmetry, restraint
6. **Surface** — subtle paper/washi tactility
7. **Readability** — clean face, clear silhouette, believable costume construction

---

## 2. Full Component List (Everything You Can Swap)

These are the **plug-and-play components** other than style.

1. Character identity
2. Role / profession / archetype
3. Core visual thesis
4. Personality read
5. Contradiction / inner tension
6. Body / age / proportions
7. Face design
8. Expression
9. Hair
10. Costume
11. Accessories
12. Shape language
13. Supernatural / power signature
14. Prop / weapon / tool
15. Pose
16. Gaze / head direction
17. Composition / framing
18. Environment / background abstraction
19. Lighting
20. Color accents beyond the locked blue
21. Surface / texture emphasis
22. Output intent / format
23. Avoid / exclusions
24. Priority stack (what must read first)

If you want consistency, do **not** vary all 24 at once. Usually change **3–8 components**, not everything.

---

## 3. Recommended Prompt Architecture

Use this structure every time.

```text
[ART STYLE CORE]

OUTPUT INTENT
[wallpaper / splash art / key visual / portrait / character sheet]

SUBJECT
[identity + age + body + core impression]

ROLE
[profession / archetype / world role]

CORE VISUAL THESIS
[one-line design thesis]

PERSONALITY READ
[5–7 outward traits]

INNER CONTRADICTION
[1–2 lines]

FACE
[feature block]

EXPRESSION
[default emotion + secondary tension]

HAIR
[cut + behavior + color]

COSTUME
[full outfit system]

ACCESSORIES
[small personal items]

SHAPE LANGUAGE
[line/shape principles]

SUPERNATURAL SIGNATURE
[power manifestation / abstract motif]

PROP / WEAPON / TOOL
[optional]

POSE
[body mechanics]

GAZE / HEAD DIRECTION
[optional if not already in pose]

COMPOSITION
[canvas organization]

ENVIRONMENT / BACKGROUND
[atmospheric context]

LIGHTING
[light logic]

COLOR ACCENTS
[optional extra color policy]

SURFACE / TEXTURE
[paper / tactile handling]

PRIORITY STACK
[1, 2, 3, 4]

AVOID
[list of exclusions]
```

---

## 4. Slot Limits Per Component

This is the important part. Yes, each component should have a limited number of slots. Otherwise the prompt becomes bloated and unstable.

### 4.1 Character identity
**Purpose:** who the subject is at a glance  
**Recommended slots:** 4
- age band
- gender presentation / identity read
- body type
- core vibe

**Template**
```text
An original [adult / late-teen / middle-aged] [female / male / androgynous / nonbinary-presenting] character in their [age range], with [body type] and a [core vibe] presence.
```

**Heavy-custom template**
```text
Character identity:
- age:
- body type:
- height impression:
- silhouette impression:
- gender presentation:
- attractiveness style:
- physical credibility notes:
```

---

### 4.2 Role / profession / archetype
**Purpose:** gives design logic  
**Recommended slots:** 3
- job / role
- allegiance / origin
- behavioral archetype

**Template**
```text
She is a [role] from [organization/faction/context], read primarily as a [behavioral archetype].
```

**Heavy-custom template**
```text
Role:
- job:
- specialization:
- organization/faction:
- rank/status:
- field conditions:
- public image:
- private reality:
```

---

### 4.3 Core visual thesis
**Purpose:** design sentence that keeps the image coherent  
**Recommended slots:** 1 sentence only

**Template**
```text
Core visual thesis: [X] transformed by [Y], resulting in [Z].
```

**Examples**
- Institutional tactical gear transformed by rebellious customization.
- Monastic discipline destabilized by unresolved grief.
- Ceremonial elegance interrupted by industrial brutality.

---

### 4.4 Personality read
**Purpose:** what the viewer feels first  
**Recommended slots:** 5 to 7 traits max

**Template**
```text
At first glance the character should feel:
- trait 1
- trait 2
- trait 3
- trait 4
- trait 5
```

**Heavy-custom template**
```text
Personality read:
- first impression:
- social energy:
- emotional regulation:
- confidence type:
- humor type:
- danger level:
- sympathy hook:
```

---

### 4.5 Contradiction / inner tension
**Purpose:** stops the design from being one-note  
**Recommended slots:** 1 primary contradiction + 1 secondary nuance

**Template**
```text
Underlying contradiction: outwardly [A], but inwardly [B].
Secondary nuance: [C].
```

---

### 4.6 Body / age / proportions
**Purpose:** physical clarity  
**Recommended slots:** 5
- age range
- build
- proportions
- height impression
- athletic language

**Template**
```text
Use believable [adult / young adult / mature] anime proportions.
The build is [lean / athletic / sturdy / elegant / wiry / muscular-but-controlled].
She should read as [height impression].
```

**Heavy-custom template**
```text
Body:
- age range:
- build:
- height impression:
- shoulder width:
- waist/hip relationship:
- limb length feel:
- athleticism type:
- realism constraints:
```

---

### 4.7 Face design
**Purpose:** face construction, not expression  
**Recommended slots:** 5 to 8 bullet points
- eye shape
- eyebrow behavior
- mouth/smile type
- makeup/grooming
- nose/jaw delicacy or strength
- maturity note

**Template**
```text
Facial characteristics:
- eye shape
- eyebrow behavior
- mouth/smile character
- makeup/grooming character
- facial maturity note
```

**Heavy-custom template**
```text
Face:
- eye shape:
- iris impression:
- eyelid / liner handling:
- eyebrow shape:
- nose:
- mouth:
- jaw/chin:
- skin finish:
- makeup:
- age signal:
```

---

### 4.8 Expression
**Purpose:** current emotional frame  
**Recommended slots:** 3
- default expression
- emotional subtext
- alternate range (optional)

**Template**
```text
Default expression: [surface emotion].
Subtext: [hidden emotional layer].
```

**Heavy-custom template**
```text
Expression:
- mouth state:
- eyebrow state:
- eye openness:
- gaze intensity:
- emotional surface:
- emotional subtext:
- alternate expressions this face should support:
```

---

### 4.9 Hair
**Purpose:** cut, movement, color  
**Recommended slots:** 6
- color
- root treatment
- length
- style
- movement behavior
- asymmetry/tint notes

**Template**
```text
Hair:
- color:
- roots:
- length:
- style:
- behavior:
- accent treatment:
```

**Heavy-custom template**
```text
Hair:
- base color:
- root color:
- secondary tint:
- length:
- silhouette shape:
- styling method:
- strand behavior:
- flyaway level:
- symmetry rules:
```

---

### 4.10 Costume
**Purpose:** outfit system  
**Recommended slots:** 8
- outfit concept
- upper layer
- inner layer
- lower layer
- footwear
- handwear
- utility/supporting elements
- realism constraints

**Core template**
```text
Costume concept: [one-line outfit identity].
Base outfit:
- upper layer:
- inner layer:
- lower layer:
- footwear:
- handwear:
- utility/support items:
- personal styling note:

Costume requirements:
- believable seams
- practical closures
- realistic fabric thickness
- readable material differences
- understandable layering
- plausible construction
```

**Heavy-custom costume template**
```text
Costume:
- design thesis:
- silhouette class:
- upper outerwear:
- upper innerwear:
- lower wear:
- legwear:
- footwear:
- gloves/handwear:
- belt/harness:
- utility storage:
- visible fasteners:
- material palette:
- wear-and-tear level:
- exposure level:
- personal customization markers:
- forbidden elements:
```

**Costume categories cheat list**
- **Silhouette class:** fitted / cropped / oversized / tapered / layered / asymmetrical / long-line / compact tactical
- **Upper outerwear:** cropped jacket / bomber / field jacket / blazer / coat / poncho / sleeveless vest / hooded shell
- **Innerwear:** fitted top / mock neck / shirt / banded wrap top / bodysuit / knit / tactical undershirt
- **Lower wear:** shorts / slim trousers / wide trousers / skirt / asymmetrical skirt / tactical pants / pleated hybrid bottom
- **Legwear:** utility tights / stockings / bare legs / wraps / compression panels
- **Footwear:** combat boots / sneakers / loafers / sandals / heeled boots / tabi-inspired boots / work boots
- **Handwear:** fingerless gloves / full gloves / wraps / bare hands / arm guards
- **Utility:** compact belt / pouch set / harness / holster / sling straps / buckle system
- **Customization markers:** patches / paint marks / mismatched straps / altered hem / stitched repair / pins / talismans / charm / ribbon / tag

**Practical limit:** do not pick more than **1–2 items** from each subcategory.

---

### 4.11 Accessories
**Purpose:** small personal signals  
**Recommended slots:** 1 to 3 only

**Template**
```text
Accessories:
- accessory 1
- accessory 2
```

**Heavy-custom template**
```text
Accessories:
- item:
- placement:
- material:
- emotional meaning:
- condition:
```

---

### 4.12 Shape language
**Purpose:** abstract design behavior  
**Recommended slots:** 4 to 6 principles

**Template**
```text
Use shape language based on:
- asymmetry
- diagonal motion
- interrupted curves
- slight visual imbalance
```

**Heavy-custom template**
```text
Shape language:
- dominant geometry:
- secondary geometry:
- curve behavior:
- symmetry rule:
- silhouette pressure points:
- recurring abstract motif:
```

---

### 4.13 Supernatural / power signature
**Purpose:** abstract VFX identity  
**Recommended slots:** 5
- meaning
- manifestation type
- shape logic
- color behavior
- restraint note

**Template**
```text
Supernatural signature:
- represents:
- manifests as:
- motion quality:
- color/material behavior:
- restraint note:
```

**Heavy-custom template**
```text
Power signature:
- symbolic meaning:
- activation mood:
- primary form language:
- secondary effect language:
- material analog:
- interaction with body:
- interaction with space:
- color behavior:
- density control:
- what to avoid:
```

---

### 4.14 Prop / weapon / tool
**Purpose:** optional supporting story object  
**Recommended slots:** 1 primary prop, optional 1 secondary prop

**Template**
```text
Primary prop: [item], used as [function], designed with [style logic].
```

**Heavy-custom template**
```text
Prop:
- item type:
- function:
- size class:
- carried how:
- material:
- wear level:
- symbolic role:
- relationship to power:
```

---

### 4.15 Pose
**Purpose:** body mechanics and gesture  
**Recommended slots:** 6
- base pose type
- weight distribution
- leg position
- arm action
- torso rotation
- motion attitude

**Core pose template**
```text
Pose:
- base pose:
- weight distribution:
- leg arrangement:
- torso action:
- arm action:
- overall energy:
```

**Heavy-custom pose template**
```text
Pose:
- overall pose type:
- support leg:
- free leg:
- hip angle:
- shoulder angle:
- torso twist:
- spine curve:
- left arm action:
- right arm action:
- hand gesture:
- head tilt:
- eye line:
- movement direction:
- emotional read through posture:
```

**Pose category cheat list**
- **Neutral but alive:** relaxed standing / asymmetrical standing / contrapposto / leaning
- **Casual expressive:** sitting on ledge / crouching / perched / kneeling / slouched seat / turning over shoulder
- **Cinematic movement:** walking forward / sudden turn / pivot / stepping through space / windup / recovery pose
- **Combat light:** evasive twist / landing / ready stance / off-balance recoil / power-casting gesture
- **Portrait-biased:** half-body turn / close bust pose / over-shoulder look / hand-near-face pose

**Pose controls cheat list**
- **Weight:** on left leg / on right leg / evenly distributed / seated weight / suspended mid-motion
- **Torso:** upright / slight twist / sharp twist / forward lean / backward lean / curved slouch
- **Arms:** one hand in pocket / hand near belt / hand extended / crossed loosely / one arm bracing / one arm casting power
- **Head:** level / slight tilt / chin up / chin down / turned away / turned back
- **Energy:** calm / cocky / poised / predatory / evasive / explosive / playful

**Practical limit:** choose **1 item per subcategory**, not all of them.

---

### 4.16 Gaze / head direction
**Purpose:** focal psychology  
**Recommended slots:** 3
- head direction
- gaze target
- gaze intensity

**Template**
```text
Head direction: [angle].
Gaze: [toward viewer / past viewer / downward / off-screen].
Intensity: [soft / direct / challenging / detached / amused].
```

---

### 4.17 Composition / framing
**Purpose:** controls the whole image  
**Recommended slots:** 6
- output format
- crop level
- figure placement
- directional flow
- detail density map
- readability rule

**Template**
```text
Composition:
- format:
- crop/framing:
- figure placement:
- directional flow:
- calm space placement:
- readability priority:
```

**Heavy-custom template**
```text
Composition:
- aspect intention:
- full-body / 3/4 / thigh-up / bust-up:
- figure position on canvas:
- negative space location:
- direction of motion:
- framing gestures:
- supporting abstract shapes:
- icon/clock safe area needs:
- edge safety rules:
```

---

### 4.18 Environment / background abstraction
**Purpose:** context without clutter  
**Recommended slots:** 4
- world hint
- abstraction level
- depth behavior
- motif support

**Template**
```text
Background should remain atmospheric and restrained, using [mist / watercolor drift / faint architecture / shadow fragments / environmental silhouettes] to suggest [context].
```

**Heavy-custom template**
```text
Environment:
- context type:
- realism level:
- background motif:
- depth layers:
- atmospheric material:
- silhouette support elements:
- clutter ceiling:
```

---

### 4.19 Lighting
**Purpose:** light logic  
**Recommended slots:** 4
- source quality
- shadow softness
- mood effect
- exclusions

**Template**
```text
Use [soft diffuse daylight / overcast light / soft side light / dim interior light] with [very soft / moderate] shadows.
Lighting should clarify the face, costume layers, and silhouette without flashy cinematic effects.
```

**Heavy-custom template**
```text
Lighting:
- source type:
- direction:
- temperature:
- contrast range:
- shadow softness:
- specularity level:
- mood goal:
- forbidden lighting traits:
```

---

### 4.20 Color accents beyond blue
**Purpose:** controlled secondary palette  
**Recommended slots:** 2
- optional accent color
- where it appears

**Template**
```text
Optional accent color: [muted warm beige / dull crimson / muted gold / desaturated violet / olive gray], used sparingly in [accessories / lining / small makeup notes / small costume details].
```

**Heavy-custom template**
```text
Accent color policy:
- accent hue:
- saturation ceiling:
- allowed surfaces:
- forbidden surfaces:
- emotional role:
```

---

### 4.21 Surface / texture emphasis
**Purpose:** tactile finish  
**Recommended slots:** 3
- paper grain level
- watercolor behavior
- ink texture behavior

**Template**
```text
Surface texture should remain subtle: light paper grain, delicate watercolor diffusion, and controlled dry-brush texture visible mostly at closer inspection.
```

---

### 4.22 Output intent / format
**Purpose:** what the image is for  
**Recommended slots:** 4
- deliverable type
- aspect ratio
- usability requirement
- crop rule

**Template**
```text
Output intent: [desktop wallpaper / smartphone wallpaper / splash illustration / promotional portrait / character sheet].
Compose specifically for [16:9 / 9:16 / 4:5 / 1:1].
```

**Heavy-custom template**
```text
Output:
- deliverable type:
- aspect ratio:
- full screen or print:
- icon-safe / clock-safe area:
- crop tolerance:
- focal placement rule:
```

---

### 4.23 Avoid / exclusions
**Purpose:** what the model must not do  
**Recommended slots:** 8 to 15

**Template**
```text
Avoid:
- item 1
- item 2
- item 3
```

**Rule:** keep this targeted. If you dump 40 unrelated bans, the prompt gets noisy.

---

### 4.24 Priority stack
**Purpose:** forces the model to know what matters most  
**Recommended slots:** 3 to 5 priorities

**Template**
```text
Visual priority:
1. face and personality
2. silhouette
3. costume design
4. supernatural signature
5. composition
```

---

## 5. Costume Cheat Sheet — Fast Fill Version

Use this when you want to design costume quickly.

```text
COSTUME QUICK FILL
Outfit identity:
Upper outerwear:
Inner top:
Lower wear:
Legwear:
Footwear:
Handwear:
Utility/support:
Personal customization:
Material feel:
Exposure level:
Forbidden motifs:
```

### Fast example
```text
Outfit identity: altered institutional field uniform with punk personalization
Upper outerwear: fitted cropped tactical bomber
Inner top: simple fitted dark inner shirt
Lower wear: high-waisted shorts over utility tights
Legwear: fitted utility tights with reinforced panels
Footwear: practical combat boots
Handwear: fingerless gloves
Utility/support: compact belt with two small pouches
Personal customization: mismatched strap ends and one stitched repair mark
Material feel: matte technical fabric with some canvas and leather accents
Exposure level: controlled, not overly revealing
Forbidden motifs: sequins, slogan shirts, copyrighted logos, iconic movie costume references
```

---

## 6. Pose Cheat Sheet — Fast Fill Version

Use this when you want to swap poses rapidly.

```text
POSE QUICK FILL
Base pose:
Weight distribution:
Leg action:
Torso angle:
Arm action:
Head angle:
Gaze:
Energy:
Motion direction:
```

### Fast example A — cocky standing
```text
Base pose: asymmetrical relaxed standing
Weight distribution: most weight on the rear leg
Leg action: front leg relaxed and slightly forward
Torso angle: slight backward lean
Arm action: one hand near the belt, the other relaxed
Head angle: slight tilt to one side
Gaze: direct eye contact with the viewer
Energy: cocky, playful, dangerous
Motion direction: subtle diagonal tension through the torso
```

### Fast example B — cinematic walking
```text
Base pose: loose forward walk
Weight distribution: transition between steps
Leg action: one leg stepping forward, the other pushing off
Torso angle: slight counter-rotation against the hips
Arm action: one hand adjusting the jacket, the other relaxed
Head angle: slightly turned toward the viewer
Gaze: direct, knowing
Energy: confident, arrogant, agile
Motion direction: clear forward movement with trailing hair and brush effects
```

### Fast example C — seated rebel
```text
Base pose: seated casually on a low ledge
Weight distribution: seated mostly on one hip
Leg action: one knee raised, the other leg hanging lower
Torso angle: slight forward curl with relaxed slouch
Arm action: one elbow resting on the raised knee
Head angle: chin slightly lowered
Gaze: looking up toward the viewer
Energy: relaxed, rebellious, self-assured
Motion direction: diagonal rhythm from bent leg through shoulder line
```

---

## 7. Master Plug-and-Play Prompt Template

Copy this block and replace only the modules you want.

```text
ART STYLE CORE
A premium contemporary Japanese anime-game illustration with elegant, airy, slightly melancholic mood.
Combine sophisticated anime character rendering with selective Japanese sumi-e brushwork, translucent watercolor, restrained supernatural abstraction, and strong negative space.
Use expressive black ink brushwork selectively as a major graphic element, with visible pressure variation, thick-to-thin transitions, occasional dry-brush texture, imperfect endings, and controlled negative space.
Use soft sky-blue watercolor as the primary atmospheric color, luminous and translucent rather than neon, with subtle pigment pooling, soft bleed, and delicate overlap with black ink.
Maintain premium modern anime-game readability: believable adult proportions, controlled cel shading, restrained painterly softness, clean facial construction, clear costume construction, readable materials, and strong silhouette.
The final image should feel like polished flagship Japanese RPG key art with subtle tactile paper/washi texture embedded into the image rather than a literal blank sheet.
Avoid neon cyberpunk aesthetics, generic magic circles, random kanji, fake Japanese text, excessive particles, heavy ink splatter, cluttered backgrounds, exaggerated anatomy, or decorative AI noise.

OUTPUT INTENT
Create a [desktop wallpaper / smartphone wallpaper / splash illustration / portrait] in [aspect ratio].

SUBJECT
An original [age band] [gender presentation] character with [body type] and [core vibe].

ROLE
The character is a [role] from [organization/faction/context], read primarily as [archetype].

CORE VISUAL THESIS
[design thesis sentence]

PERSONALITY READ
At first glance the character should feel:
- [trait 1]
- [trait 2]
- [trait 3]
- [trait 4]
- [trait 5]

INNER CONTRADICTION
Outwardly [A], but inwardly [B].
Secondary nuance: [C].

BODY
Use believable [adult / mature / young-adult] anime proportions.
The build is [build].
Height impression: [height read].

FACE
Facial characteristics:
- [feature 1]
- [feature 2]
- [feature 3]
- [feature 4]
- [feature 5]

EXPRESSION
Default expression: [expression].
Subtext: [subtext].

HAIR
Hair:
- base color: [ ]
- roots: [ ]
- length: [ ]
- style: [ ]
- behavior: [ ]
- accent treatment: [ ]

COSTUME
Costume concept: [ ].
Base outfit:
- upper layer: [ ]
- inner layer: [ ]
- lower layer: [ ]
- footwear: [ ]
- handwear: [ ]
- utility/support items: [ ]
- personal styling note: [ ]
Costume requirements:
- believable seams
- practical closures
- realistic fabric thickness
- readable material differences
- understandable layering
- plausible construction

ACCESSORIES
- [ ]
- [ ]

SHAPE LANGUAGE
Use shape language based on:
- [ ]
- [ ]
- [ ]
- [ ]

SUPERNATURAL SIGNATURE
Represents: [ ].
Manifests as:
- [ ]
- [ ]
- [ ]
Motion quality: [ ].
Restraint note: [ ].

PROP / TOOL
Primary prop: [optional].

POSE
- base pose: [ ]
- weight distribution: [ ]
- leg arrangement: [ ]
- torso action: [ ]
- arm action: [ ]
- overall energy: [ ]

GAZE / HEAD DIRECTION
Head direction: [ ].
Gaze: [ ].
Intensity: [ ].

COMPOSITION
- framing: [full-body / 3/4 / thigh-up / bust-up]
- figure placement: [ ]
- motion flow: [ ]
- negative space placement: [ ]
- detail concentration: [ ]
- safe area note: [optional]

ENVIRONMENT / BACKGROUND
Use restrained atmospheric background elements such as [ ] to suggest [ ].
Keep the background supportive, layered, and uncluttered.

LIGHTING
Use [ ] with [ ] shadows.
Lighting should clarify the face, costume layers, and silhouette without flashy cinematic effects.

ACCENT COLOR POLICY
Optional accent color: [ ], used only in [ ].

SURFACE / TEXTURE
Retain subtle paper grain, delicate watercolor diffusion, and controlled dry-brush texture.

PRIORITY STACK
1. [ ]
2. [ ]
3. [ ]
4. [ ]

AVOID
- [ ]
- [ ]
- [ ]
- [ ]
```

---

## 8. Character Cheatsheet Template

This is the shorter sheet. Fill this first before writing the full prompt.

```text
CHARACTER CHEATSHEET
Name / codename:
Age band:
Gender presentation:
Body type:
Height impression:
Core vibe:
Role:
Archetype:
Core contradiction:

PERSONALITY
- 
- 
- 
- 
- 

FACE
Eyes:
Eyebrows:
Mouth / smile:
Makeup / grooming:
Facial maturity note:

HAIR
Base color:
Roots:
Length:
Style:
Behavior:
Accent tint:

COSTUME
Outfit thesis:
Upper outerwear:
Inner top:
Lower wear:
Legwear:
Footwear:
Handwear:
Utility/support:
Accessories:
Customization markers:
Material feel:

POWER / SIGNATURE
Meaning:
Visual motif:
Effect behavior:
Color behavior:
Restraint note:

POSE
Base pose:
Weight distribution:
Torso action:
Arm action:
Head angle:
Gaze:
Energy:

COMPOSITION
Output:
Aspect ratio:
Framing:
Figure placement:
Negative space:
Background suggestion:
Lighting:
Priority stack:

AVOID
- 
- 
- 
```

---

## 9. Heavy-Custom Component Template

Use this when a component needs serious specificity.

```text
[COMPONENT NAME]
Primary goal:
What must read immediately:
Structural rules:
Material rules:
Motion/behavior rules:
Emotional rules:
What may vary:
What must stay fixed:
What to avoid:
Priority within image:
```

### Example — heavy custom costume
```text
COSTUME
Primary goal: communicate altered institutional field gear with rebellious personal customization.
What must read immediately: practical field-operator function and stylish asymmetry.
Structural rules: clear layering, believable closures, usable pockets, compact silhouette.
Material rules: matte technical fabric, canvas accents, limited leather reinforcement.
Motion/behavior rules: jacket hem and straps respond clearly to body movement.
Emotional rules: should feel playful, impulsive, and slightly aggressive.
What may vary: exact jacket cut, shorts vs slim trousers, pouch placement.
What must stay fixed: practical construction, tactical roots, controlled exposure.
What to avoid: sequins, slogan graphics, copyrighted logos, iconic movie costume cues.
Priority within image: high.
```

### Example — heavy custom pose
```text
POSE
Primary goal: communicate relaxed danger and impulsive confidence.
What must read immediately: asymmetrical posture and comfort inside chaos.
Structural rules: readable weight distribution, clear torso twist, visible hand gesture, stable leg logic.
Material rules: hair, jacket, and effects must react to the same directional force.
Motion/behavior rules: body movement should guide the direction of ink and watercolor effects.
Emotional rules: amused, cocky, dangerous, but not stiff.
What may vary: standing / seated / walking / evasive.
What must stay fixed: asymmetry, readability, kinetic rhythm.
What to avoid: symmetrical posing, fashion-model stiffness, generic hero stance.
Priority within image: high.
```

---

## 10. Best Practice Rules

1. **Lock the style core.** Do not rewrite it every time unless the whole series changes.
2. **Write a cheatsheet first.** Then expand into a full prompt.
3. **Swap modules, not sentences.** Replace the entire `POSE` or `COSTUME` block instead of editing scattered lines.
4. **Limit slots.** More detail is not always better. Too many options degrade clarity.
5. **Always include a priority stack.** It prevents the model from wasting detail on the wrong area.
6. **Use one design thesis sentence.** If you cannot explain the design thesis in one line, the concept is probably still vague.
7. **Use heavy-custom templates only when needed.** Most of the time, quick-fill blocks are enough.
8. **For consistency across multiple images, keep these fixed:** style core, body proportions, face logic, hair logic, costume thesis, power motif.
9. **For variation, change these first:** pose, expression, framing, prop, background hint, lighting nuance.
10. **If the image starts drifting, shorten the prompt.** Noise often comes from over-specification.

---

## 11. What To Keep Fixed vs What To Change

### Keep fixed for same-character consistency
- style core
- body type
- face design
- hair logic
- costume thesis
- core personality contradiction
- supernatural motif

### Safe to change often
- pose
- expression
- framing
- background abstraction
- output format
- prop/tool
- lighting nuance
- small accessories

### Change carefully
- costume silhouette
- age read
- color accent system
- power behavior
- proportions

---

## 12. Minimum Viable Fill Set

If you want speed, fill only these:
- output intent
- subject
- role
- core visual thesis
- personality read
- face
- hair
- costume
- power signature
- pose
- composition
- lighting
- avoid

That is enough for a strong result.

---

## 13. First Modules To Build

You asked to start with **costume** and **pose** first. Correct choice. Those are the two most useful plug-and-play modules after the style core.

If you continue this system later, build the next modules in this order:
1. face
2. hair
3. power signature
4. composition
5. role/archetype
6. accessories/prop
7. environment
8. lighting

