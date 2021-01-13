const FILTER_TYPE = 'user_paths';
var map_property = {zoom: 4, popup_bool: false};

$(document)
.on('click', '.js_path_delete', async function(){
  await get_translate('path_delete_confirm');
  if (confirm(alert_text)){
    delete_path($(this).data('path_db_id'));
  }
})

function delete_path($id, $reload = true){
  $.ajax({
    type: "post",
    url: "index.php",
    data: {
      act: 'delete_path',
      id: $id,
      reload: $reload
    },
  }).done(function(result){
    data = JSON.parse(result);
    if (data.error=='1'){
      alert(data.errorText);
    } else {
      if(data.reload=='true'){
        location.reload();
      }
    }
  })
}
