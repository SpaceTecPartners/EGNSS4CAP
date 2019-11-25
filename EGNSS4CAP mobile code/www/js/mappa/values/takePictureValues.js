var take_picture_menus = {
  'pics':{'state':'closed',
    'btns':{
      'open_gallery': {'icon':'ion-android-image', 'label':'Apri galleria'},
      'take_picture':{'icon':'ion-android-camera', 'label':'Scatta foto'}
    }
  },
  'zoom':{'state':'closed',
    'btns':{
      'on_me': {'icon':'ion-pinpoint', 'label':'Zoom alla posizione'},
      'on_path': {'icon':'ion-android-expand', 'label':'Zoom alla distanza'},
      'layer_visibility': {'icon':'ion-eye', 'label':'Visibilità layer'},

    }
  },
  'server': {
    'state': 'closed',
    'btns': {
      'upload_pics': {'icon': 'ion-upload', 'label': 'Salva'},
      'download_pics': {'icon': 'ion-archive', 'label': 'Scarica'}
    }
  },
  'gnss':{'state':'closed',
    'btns':{
      'gnss_info': {'icon':'ion-eye', 'label':'Info Gnss'}
    }
  }
};

var pics_menu_states = {'open':'open', 'close':'closed'};
var pics_loading_status = {'pos': false, 'part': false};

angular.module('mappa.module')
    .value('take_picture_menus',take_picture_menus)
  .value('pics_menu_states',pics_menu_states)
  .value('pics_loading_status',pics_loading_status);
