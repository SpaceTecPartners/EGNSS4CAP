
$(document)
  .ready(function() {
    load_table();
  })
  .on('click', '.js_agency_detail', function() {
    var id = $(this).attr("data-href");
    window.location.href = "index.php?act=pa_officers_list&id=" + id;
  })
  .on('click', '.js_new_agency_show', function(){
    show_hide_new_agency_inputs(this);
  })
  .on('click', '.js_button_close_agency_edit', function(){
    hide_new_agency_inputs(this);
  })

function load_table(){
  $.ajax({
    type: "post",
    url: "index.php",
    async: true,
    data: {
      act: "load_agency_table"
    },
    dataType: "HTML",
    success: function (agency_tbody) {
      $('.js_table tbody').html(agency_tbody);
    }
  })
}

function show_hide_new_agency_inputs ($button){
  $form = $('.js_new_agency_inputs');
  scrollToTop();
  if($form.is(":hidden")){
    clear_form($form);
    $form.show();
    $($button).hide();
  }else{
    clear_form($('.js_new_agency_inputs'));
    $('.js_new_agency_show').hide();
  }
}

function hide_new_agency_inputs (){
  $('.js_new_agency_inputs').hide();
  $('.js_new_agency_show').show();
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
