const FILTER_TYPE = 'task';
const PDF_PREPARE = false;
var map_property = {zoom:4, popup_bool: true};
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
    post = $.post("index.php", {act: "list_sort", page: 'task_list', sort: $field});
    post.done(function(data){
        if (data == '1'){
            location.reload();
        }
    });
  })
  .on("click", "[class^='clickResetSort']", function(){
    post = $.post("index.php", {act: "list_sort", page: 'task_list', sort: 'reset'});
    post.done(function(data){
        if (data == '1'){
            location.reload();
        }
    });
  })
  .on("click", "[class^='changeFilter'], [class^='form-check-input changeFilter'], .changeFilter", function(){
    $field = $(this).data('field');
    $fieldType = $(this).data('fieldtype');
    $value = 0;
    if ($(this).prop("checked")){
      $value = 1;
    }
    post = $.post("index.php", {act: "list_filter", page: 'task_list', filter: $field, type: $fieldType, value: $value});
    post.done(function(data){
        if (data == '1'){
           location.reload();
        }
    });
  });

  /*.on("click", "[class^='clickResetFilter']", function(){
            post = $.post("index.php", {filter: 'reset'});
            post.done(function(data){
                if (data == '1'){
                    location.reload();
                }
            });
        });
  })*/

function load_table(){
  $.ajax({
    type: "post",
    url: "index.php",
    async: true,
    data: {
      act: "load_task_table",
      id: location.search.split('id=')[1],
      search: $('.js_search_input').val()
    },
    dataType: "HTML",
    success: function (task_tbody) {
      $('.js_table tbody').html(task_tbody);
      var hash = window.location.hash.substr(1);
      if(hash.length > 0){
        rowOffset = $("#"+hash).offset().top;
        scrollToTop(rowOffset-100);
      }

      initMap();
    }
  })
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
