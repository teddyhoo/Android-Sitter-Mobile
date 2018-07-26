README - Android Release Notes

New Pet Care Report
New Server option settings and layout
Text message visit report option
Document attachment can be opened in sitter mobile app
Custom fields are in the specified order
Poll sync, locks screen so no touches accepted
Detail fields added: notes, parking, directions


MainApplication ---------- MainActivity
				|
				|------------LoginActivity
						|
						|
	RecyclerView		------------------
		|
		|
		------------VisitAdapter
				|
				|------------View Holder 
				|		|
				|		|
				|		-------------ViewVisitDetail
				|		|
				|		|
				|		___________PetDetailActivity
				|
				------------------------------PhotoActiivty
				|
				|
				------------------------------VisitReport

Singletons

TrackerServiceSitter
VisitsAndTracking
	|
	|
	----------------VisitDetail
	|
	|
	_____________ClientDetail
	