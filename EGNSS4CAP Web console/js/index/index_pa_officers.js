var alert_text, db_data;

$(document)
  .ready(function() {
    load_table();
  })
  .on('click', '.js_button_user_deactivate', async function(){
    $id = $(this).data('userid');
    await get_translate('officer_deactivate_confirm');
    if (confirm(alert_text)){
      deactivate_officer($id);
    }
  })

function load_table(){
  $.ajax({
    type: "post",
    url: "index.php",
    async: true,
    data: {
      act: "load_pa_officer_table",
      id: $('.js_search_input').val()
    },
    dataType: "HTML",
    success: function (officers_tbody) {
      $('.js_table tbody').html(officers_tbody);
    }
  })
}

function deactivate_officer($id){
  $.ajax({
    type: "post",
    url: "index.php",
    data: {
      act: 'deactivate_officer',
      id: $id
    },
  }).done(function(result){
    data = JSON.parse(result);
    if (data.error=='1'){
      alert(data.errorText);
    } else {
      location.reload();
    }
  })
}
