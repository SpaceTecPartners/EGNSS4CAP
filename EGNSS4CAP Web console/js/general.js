$(document).ready(function() {

  var timer;
  $(".tt").hover(function(){
    clearTimeout(timer);
    var elem = this;
    timer = setTimeout(function() {
      $(elem).find('.tt_body').fadeIn(250);
    }, 750);
  }, function(){
    $(this).find('.tt_body').hide();
    clearTimeout(timer);
  });

  $(window).resize(function() {
    clearTimeout(resizeId);
    resizeId = setTimeout(browser_resized(), 500);
    media_query(size);
  });
  $(document).on(touchClickEvent, '.js_lang', function(){
    change_language($(this).attr('id'));
  })
  $(document).on(touchClickEvent, '.show_password', function(){
    var pass_input = $(this).parent().find('input');
    if ($(pass_input).is(':password')) {$(pass_input).prop('type', 'text');} else {$(pass_input).prop('type', 'password');}
  })
  $(document).on(touchClickEvent, '.map_dropdown_btn', function(){
    if ($('#map').is(":visible")) {
      $('#map').slideUp(500);
      $('.map_dropdown_btn .btn_show').css({'display':'inline'});
      $('.map_dropdown_btn .btn_hide').hide();
    } else {
      $('#map').slideDown(500);
      $('.map_dropdown_btn .btn_show').hide();
      $('.map_dropdown_btn .btn_hide').css({'display':'inline'});
    }
  })
  $(document).on(touchClickEvent, '.navbar .navbar-toggler', function(){
    $('.navbar .navbar-collapse').toggle();
  })

})
var resizeId;
var touchClickEvent = 'ontouchstart' in window ? 'touchstart' : 'click';


function change_language(lang){
  $.ajax({
    type: 'POST',
    async: true,
    url: 'login.php',
    data: {
      act: 'lang_' + lang,
    }
  }).done(function(){
    location.reload();
  })
}

function load_responsive_table() {//loads header labels(data-label) for responsive view
  /*
  if it isn't possible to load language into js-loaded table, then after the ajax loads its html into tbody, insert this:
                  if ($('.table_responsive_load').length) {load_responsive_table();}
  -->> this will load headings into data-label, which are displayed in lower screen size.
  */
  data_to_load = [];
  $(".table_responsive_load").each(function(i01){
    data_to_load[i01] = [];
    thistable = this;
    $(thistable).find('thead tr th').each(function(i02){
      data_to_load[i01].push($(this).text());
    });
  });
  $.each(data_to_load, function(i03, arr) {
    $.each(arr, function(i04, val) {
      $('.table_responsive_load:nth-child('+(i03+1)+') tbody td:nth-child('+(i04+1)+')').attr('data-label', val);
    });
  });
}

function browser_resized() {}

//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
