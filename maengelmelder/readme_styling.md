## Styling the Mängelmelder module

### Banners

The banner shown in left Menu in Overview Map page can be changed by adding `mm_banner.png` to `res/drawable` folder

### Module title and subtitle

Title and subtitle are set when launching the module's activity
```kotlin
MMInitiator(this)
    .setConfig(MMInitiator.Config.APP_TITLE, "module title")
    .setConfig(MMInitiator.Config.APP_SUBTITLE, "module subtitle")
    .start()
```

### Colors

Color overriding can be done through your main app's `color.xml`
```xml
<color name="<code>">#ff2245</color>
```
Some pre-defined color codes:
* darkblue : `#000080`
* white : `#ffffff`
* gray : `#7e7e7e`
* lightgray : `#50d3d3d3`
* mm_cyan : `#357f8c`
* green : `#00ff00`
* lightblue : `#ADD8E6`

The list of codes:

| Code                                  | default color | Used in                                                                                   |
|---------------------------------------|---------------|-------------------------------------------------------------------------------------------|
| mmcolor_category_listitem_normal_bg   | white         | The color of category list item's background when not clicked                             |
| mmcolor_category_listitem_normal_text | gray          | The color of category list item's text when not clicked                                   |
| mmcolor_category_listitem_chosen_bg   | darkblue      | The color of category list item's background when chosen                                  |
| mmcolor_category_listitem_chosen_text | white         | The color of category list item's text when chosen                                        |
| mmcolor_step_incomplete_icontint      | lightgray     | The color of incomplete message step icon, shown when trying to upload incomplete message |
| mmcolor_step_completed_icontint       | mm_cyan       | The color of completed message step icon, shown when trying to upload incomplete message  |
| mmcolor_detail_textcolor              | white         | The color of the first text on message bubble when a marker on map is clicked             |
| mmcolor_detail_text2color             | white         | The color of the second text on message bubble when a marker on map is clicked            |
| mmcolor_slidingmenu_bg                | mm_cyan       | The color of the sliding menu's background (left side menu on Overview map)               |
| mmcolor_slidingmenu_text              | white         | The color of the sliding menu's text (left side menu on Overview map)                     |
| mmcolor_slidingmenu_banner_bg         | white         | The color of the sliding menu's banner background (left side menu on Overview map)        |
| mmcolor_bottomtoolbar_bg              | darkblue      | The color of the bottom toolbar's background in overview map                              |
| mmcolor_actionbar_bg                  | darkblue      | The color of the actionbar's background                                                   |
| mmcolor_actionbar_title_text          | white         | The color of the actionbar's title text                                                   |
| mmcolor_actionbar_subtitle_text       | white         | The color of the actionbar's subtitle text                                                |
| mmcolor_showcase_bg                   | mm_cyan       | The color of the showcase's background (tutorial layout)                                  |
| mmcolor_showcase_circle               | darkblue      | The color of the showcase's circular target (tutorial layout)                             |
| mmcolor_showcase_title_text           | white         | The color of the showcase's title text (tutorial layout)                                  |
| mmcolor_showcase_content_text         | white         | The color of the showcase's subtitle text (tutorial layout)                               |
| mmcolor_status_bg                     | darkblue      | The color of progress status's background color (e.g. when loading messages on map)       |
| mmcolor_status_text                   | white         | The color of progress status's text color (e.g. when loading messages on map)             |
| mmcolor_progress_tint                 | lightblue     | The color of progress bar in webview                                                      |
| mmcolor_icon_tint_1                   | white         | General icon tint color                                                                   |
| mmcolor_icon_tint_on_white            | darkblue      | General icon tint color on white surface                                                  |
| mmcolor_tab_selected_bg               | darkblue      | Color of selected message step indicator on the bottom (background)                       |
| mmcolor_tab_selected_text             | white         | Color of selected message step indicator on the bottom (text)                             |
| mmcolor_tab_normal_bg                 | lightblue     | Color of unselected message step indicator on the bottom (background)                     |
| mmcolor_tab_normal_text               | darkblue      | Color of unselected message step indicator on the bottom (text)                           |
| mmcolor_btn_normal_bg                 | darkblue      | General button background color                                                           |
| mmcolor_btn_normal_text               | white         | General button text color                                                                 |

### Text

Color overriding can be done through your main app's `strings.xml`
```xml
<string name="<text code>">This will override module text</string>
```

| Code                                  | Used in                                                                                                                     |
|---------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| title_welcome                         | Title of the welcome text (only shown once at the start per installation)                                                   |
| title_terms                           | Title of the "Terms of Use" page                                                                                            |
| title_policy                          | Title of the "Privacy policy" page                                                                                          |
| warn_not_accepting_terms_and_policy   | Warning when user does not accept both terms of use and privacy policy                                                      |
| step_choose_location                  | Title of the message creation step for choosing position                                                                    |
| step_choose_photo                     | Title of the message creation step for choosing photo                                                                       |
| step_choose_category                  | Title of the message creation step for choosing category                                                                    |
| step_edit_attributes                  | Title of the message creation step for editing message attributes                                                           |
| step_review                           | Title of the message creation's final review step before submitting                                                         |
| step_check_duplicates                 | Title of the message dialog for showing possible duplicates of message                                                      |
| activity_mymessages                   | Title of the "Meine Meldungen" activity, used to show list of user's messages                                               |
| activity_messagedetail                | Title of the message detail page                                                                                            |
| activity_update_message               | Title of the page where user submits comment to a message                                                                   |
| activity_new_message                  | Title of the message creation page                                                                                          |
| err_no_conn                           | Error message for when internet connection is not available                                                                 |
| err_not_found                         | Error message for 404                                                                                                       |
| err_auth                              | Error message for failed authentication when making requests                                                                |
| err_server_500                        | 500 error code                                                                                                              |
| err_server_504                        | error message when the server is not available                                                                              |
| err_server_no_respond                 | error message when server fails to respond to a request                                                                     |
| err_server_cert                       | Error message for mismatched/invalid certificate                                                                            |
| btn_new_message                       | Button text for creating new message                                                                                        |
| loading_retrieving_location           | Status text when the app is retrieving user's latest location                                                               |
| activity_profile                      | Profile activity page title                                                                                                 |
| info_message_saved                    | Status text after user saves a message for later submission/editing                                                         |
| err_msgcreation_invalid_json          | Error text after submission attempt. Missing description or some attributes                                                 |
| err_msgcreation_invalid_type          | Error text after submission attempt. Missing category                                                                       |
| err_msgcreation_invalid_pos           | Error text after submission attempt. Invalid position for the message                                                       |
| msg_creation_connection_not_available | Status text when internet connection is missing while creating a message                                                    |
| camera                                | Button text to open camera                                                                                                  |
| gallery                               | Button text to open gallery                                                                                                 |
| view_street                           | Button text for changing map type to street map                                                                             |
| view_satellite                        | Button text for changing map type to satellite                                                                              |
| position_set                          | Button text for setting user's position during message creation                                                             |
| position_instructions                 | Instruction text for position step in message creation                                                                      |
| warn_domain_not_allowed               | Error message when user tries to set a position outside of allowed bounds                                                   |
| warn_firsttime_not_emergency_app      | Warning message that is shown after first time installation. Only applicable if `ShowWarningNotEmergencyService` is enabled |