# Dewbe
WebService API, Network + DB. 
Youtube API is one of examples, this architecture can be used in any web API.
Points:
      Pagination-- PagedList; 
      Room; 
      Retrofit2; 
      LiveData; 
      Repository; 
      ViewModel; 
      Youtube DATA API; 
      Youtube Android Player API;
The app uses ViewModel to abstract the data from UI and MovieRepository as single source of truth for data. MovieRepository first fetch the data from database if exist than display data to the user and at the same time it also fetches data from the webservice and update the result in database and reflect the changes to UI from database.

![](https://github.com/burhanrashid52/YoutubeAnimation/blob/master/gifs/archtiture.png)
