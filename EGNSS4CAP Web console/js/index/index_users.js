const FILTER_TYPE = 'user';
const PDF_PREPARE = false;
var map_property = {zoom: 4, popup_bool: false};
$(document)
  .ready(function() {
    load_table();
  })
  .on('click', '.js_search', function(){
    load_table();
  })
  .on('keypress', '.js_search_input', function(e){
    if(e.which == 13) {
      e.preventDefault();
      load_table();
    }
  })
  .on("click", "[class^='clicksort'], [class^='form-check-input clicksort'], .clicksort", function(){
    $field = $(this).data('field');
    post = $.post("index.php", {act: "list_sort", page: 'user_list', sort: $field});
    post.done(function(data){
        if (data == '1'){
            location.reload();
        }
    });
  })
  .on("click", "[class^='clickResetSort']", function(){
    post = $.post("index.php", {act: "list_sort", page: 'user_list', sort: 'reset'});
    post.done(function(data){
        if (data == '1'){
            location.reload();
        }
    });
  })

function load_table(){
  $.ajax({
    type: "post",
    url: "index.php",
    async: true,
    data: {
      act: "load_users_table",
      search: $('.js_search_input').val()
    },
    dataType: "HTML",
    success: function (user_tbody) {
      $('.js_table tbody').html(user_tbody);
      var hash = window.location.hash.substr(1);
      if(hash.length > 0){
        rowOffset = $('#'+hash).offset().top;
        scrollToTop(rowOffset-100);
      }
      initMap();
    }
  })
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
