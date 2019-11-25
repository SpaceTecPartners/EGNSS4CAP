var track_path_menus = {
  'track':{'state':'closed',
    'btns':{
    'rem_all_points': {'icon':'ion-skip-backward', 'label':'Annulla tracking'},
    'rem_last_point':{'icon':'ion-arrow-left-b', 'label':'Annulla ultimo marker'},
    'add_curr_point': {'icon':'ion-disc', 'label':'Aggiungi nuovo marker'}
  }

  },
  'zoom':{'state':'closed',
    'btns':{
    'on_me': {'icon':'ion-pinpoint', 'label':'Zoom alla posizione'},
    'on_path': {'icon':'ion-android-expand', 'label':'Zoom alla distanza'},
    'layer_visibility': {'icon':'ion-eye', 'label':'Visibilità layer'}
  }
  },
  'control':{'state':'closed',
    'btns':{
    'toggle_track': {'icon':'ion-play', 'label':'Start'},
    'pause_track':{'icon':'ion-pause', 'label':'Pausa'}
  }
  },
  'server':{'state':'closed',
    'btns':{
    'upload_tracks':{'icon':'ion-upload', 'label':'Salva'},
    'download_tracks': {'icon':'ion-archive', 'label':'Scarica'}
  },
    'gnss':{'state':'closed'}
  }
};

var track_path_menu_states = {'open':'open', 'close':'closed'};
var track_path_loading_status= {'pos': false, 'part': false};
angular.module('mappa.module')
    .value('track_path_menus', track_path_menus)
  .value('track_path_menu_states', track_path_menu_states)
    .value('track_path_loading_status', track_path_loading_status);
