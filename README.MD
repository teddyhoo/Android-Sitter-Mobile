ANDROID RELEASE NOTES (8-15-18)

BUG FIXES: 

- firstLogin param in visit request call now properly reflects if first login or polling sync, 
- back button tap on visit report that is being edited now saves the visit report info 
- list view locked when coming to foreground, whilst awaiting update; message displays indicating update in progress
- method of updating list view when coming to the foreground (had calls to recyclerView .setAdapter, .swapAdapter mixed up)
- null value check on location coordinate being added to visit (transition background to foreground state issue; the location tracker may report location before the visit data object is build / ready to receive)
- null value check on recyclerView (visit list table)
- the last three bug fixes were related to background to foreground transition issues

MAJOR CHANGES: 

1. DOCUMENT ATTACHMENT FOR CUSTOM CLIENT AND PET FIELDS: if document attachment type of custom field, icon appears in respective detail views. Tapping icon will invoke WebView Mime handlers and show document in WebView within the native app. Note, if the user does not have a handler for document type (e.g. no reader for msword), the app will not be able to launch a view for it. Document attachment fields are summary listed in the manager note slide up view (tap the yellow sticky note icon in the main list view).

2. DETAIL VIEWS UI: as per Android best practices for newer versions of OS, LinearLayout views have been converted to ConstraintLayout which is (a) faster and more efficient to render (b) more reliably handles relative sizing for different screen formats. Additionally, the number of custom fields was hard-coded at a maximum of 40 due to explicitly laying out in XML format (and removing excess fields when the profile had < 40). This has been changed so that custom fields are programmtically added to the layout. In short, just about every view in the app was converted over to ConstraintLayout and, where possible / feasible, the layouts were tuned for consistency across device screen sizes. 

3. CLIENT DETAIL FIELDS ORDERING AND MISSING: Missing fields added for detail views: Notes (Basic Info tab), Parking, Driving Directions. Custom fields are displayed in order according to the dictionary keys:

e.g. custom1 --> { label = <name>, value = <field value>}
	
The custom<#> indicates ordering.

4. POLL SYNC / BACKGROUND / FOREGROUND TRANSITIONS: the transition from background to foreground seeemed to introduce quite a bit of instability causing many inexplicable crashes. There are many possible reasons for crash like behavior (or exceptions). There is a lot of stuff going on when you transition to the background and come to the foreground. 

- location tracking in background is a big deal in terms of rights management; there are very specific compliance requirements to adhere to when running background location tracking, including permissions management, generating foreground notification when going to background; removing message when coming to the foreground

- the app does several things when coming to foreground:
   - reviews the bad request queue items and attempts to re-transmit failed requests
   - makes a fresh request for visit data, then updates, overwrites main list view
   - manage the views, determining null ness; the views are intended to be recycled as per mobile os best practices; tearing them down and rebuilding is sometimes not as straightforward as would seem
   - converts location tracker from foreground state to background (the tracker state is always opposite of the user state)
   - re-building visit objects, with requirement that any phone data that is not on the server needs to be pulled from persistent storage (file read)
   - the app does these things on multiple threads in order to provide responsive experience
   
In short, the coming foreground logic is fairly involved. There are numerous steps involved. Each step needs to choreographed in multiple threads. I have modeled and flow charted in detail all the steps and reviewed and debugged each one to ensure that what was expected was what was occuring etc. There were a few minor tweaks where potential issues could arise. And they have been corrected. 

The most significant change was reworking the network checking code. All of the above needs to be orchestrated but if there is no network connection, there is no point in tear down and rebuild. Sometimes it might appear that there is a network connection (e.g. captive network) and this caused confusion within the app. Checking for captive network access has now been added / tested. And network code has been streamlined. 
   
5. SERVER SETTING OPTIONS:

	- Pet Care Report - NEW TEMPLATE VERSION with a link to online view and navigation to history of reports
	- Pet Care Report sent via text message through Twilio
	- Preference setting options: minimumArriveEarly, multiVisit
