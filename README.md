# MultiStateAnimation

Android library to create complex multi-state animations.

![Demo animation](docs/images/demo1.gif)

## Overview

A class that allows for complex multi-state animations using Android's
`AnimationDrawable`. It supports both oneshot and looped animation sections.
Transitions between sections can be defined, and will play automatically when
moving between the defined states. State transitions can be queued
asynchronously, and the library will take car of smoothly starting the next
animation once the current section is complete.

## Usage

Animations are defined using a JSON file included as a `raw` resource. Each
animation consists of a series of states. A state has some metadata and a list
of frames to draw. A state can also define transitions from other states. A
transition is a list of frames that will be played when moving directly from a
specified state to the state where the transition is defined.

### Example JSON animation definition

For illustrative purposes, the following example is annotated with javascript-style comments. However,
because the Android JSON parser is used in this project,
**comments in JSON resource files are not supported**!

```javascript

    {
        // The ID is used in code to specify a section to play
        "first_section": { 
            // If true, this section will play once and stop. Otherwise it
            // will loop indefinitely.
            "oneshot": false, 
            // The number of milliseconds that each frame of this section will
            // play
            "frame_duration": 33, 
            // Each frame is the name of an image resource. They will be
            // played in the order defined.
            "frames": [
                "frame_01",
                "frame_02"
            ],

        }

        // A section with a single frame and "oneshot" set to true is
        // equivalent to a static image.
        "second_section": {
            "oneshot": true,
            "frames": [
                "other_frame_01"
            ],
            // An optional set of transitions.
            "transitions_from": {
                // The frames of a transition will be played before playing
                // the normal frames of this section when transitioning. In
                // this case, the frames for this transition will play if
                // "first_section" is playing when
                // queueTransition("second_section") is called
                "first_section": {
                    // Each section and transition can optionally define their
                    // own frame duration.
                    "frame_duration": 33,
                    "frames": [
                            "first_to_second_transition_001",
                            "first_to_second_transition_002"
                    ]
                }
                // As a special case, a transition ID of "" is a transition
                // from nothing. It will play if the associated section is the
                // first to ever play.
                "": {
                    "frames": [
                        "nothing_to_second_001",
                        "nothing_to_second_002"
                    ]
                }
            }
        }
    }

```

### Java API

From your Android code, create an instance of `MultiStateAnimation`. You
will typically use the constructor function `fromJSONResource` .  you can use
`queueTransition` and `transitionNow` from the GUI thread to start playing the
animations.

| Method | Description |
|--------|-------------|
| `int currentSectionDuration()` | Calculates the total duration of the current animation section, including the transition if applicable. |
| `static MultiStateAnimation fromJsonResource(Context context, View view, int resid)` | Creates a new MultiStateAnimation object from a json string. This constructor function takes a `Context`, a `View` that will have it's background set to the playing animations, and the resource ID of the JSON file defining the animation. |
| `void queueTransition(String id)` | Queues a section to start as soon as the current animation finishes. If no animation is currently playing, the queued transition will start immediately. |
| `void transitionNow(String id)` | Starts a specific section without waiting for the current animation to finish. |
| `AnimationDrawable getCurrentDrawable()` | Returns the currently playing animation, or null if no animation has ever played. |
| `String getCurrentSectionId()` | Return the ID of the current section if one is playing, or null otherwise. |
| `String getTransitioningFromId()` | If the currently playing animation is a transition, return the ID if the section that this is transitioning from. Return null if no animation is playing, or the currently playing animation is not a transition. |
| `void setSeriesAnimationFinishedListener(AnimationSeriesListener listener)` | Registers a listener that will be called when a running animation finishes. |
| `AnimationSeriesListener getSeriesAnimationFinishedListener()` | Returns the registered listener. |

### Example Usage

See the [main Activity](samples/src/main/java/com/getkeepsafe/android/multistateanimation/samples/ThreeStateSampleActivity.java)
 in the [sample application](samples/) for an example.

## License

    Copyright 2015 KeepSafe

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.