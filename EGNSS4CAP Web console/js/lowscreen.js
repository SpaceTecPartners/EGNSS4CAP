//small_screen-specific functions
$(document).ready(function(){

  // Check if you support touch or click:
  var touchClickEvent = 'ontouchstart' in window ? 'touchstart' : 'click';

  media_query(size);
  size.addListener(media_query) // Attach listener function on state changes

});
var size = window.matchMedia("(max-width: 575.98px)");
var state = (size.matches) ? 'default' : 'lowscreen';



function media_query(size) {
  if (size.matches && state === 'default') {//less or equal to set size and triggered once
    //console.log('lowscreen');
    state = 'lowscreen';
    $('body').addClass('body_lowscreen');

  } else if (!size.matches && state === 'lowscreen') {//greater than set size and triggered once
    //console.log('default');
    state = 'default';
    $('body').removeClass('body_lowscreen');

  } else if (size.matches) {//less or equal to set size and triggered often-f.ex. for ajax-loaded content
    //console.log('lowscreen 02');

  } else if (!size.matches) {
    //console.log('default 02');
  }
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
