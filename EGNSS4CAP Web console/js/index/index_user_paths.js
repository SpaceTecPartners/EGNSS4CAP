const FILTER_TYPE = 'user_paths';
const PDF_PREPARE = false;
var map_property = {zoom: 4, popup_bool: false};

$(document)
.on('click', '.js_path_delete', async function(){
  await get_translate('path_delete_confirm');
  if (confirm(alert_text)){
    delete_path($(this).data('path_db_id'));
  }
})
.on('click', '.js_path_export_kml', async function(){
  export_to_kml($(this).data('path_db_id'),$(this).data('path_db_name'),$(this).data('path_db_desc'));
})
.on('change', '.js_path_show_on_map', function(){
  showSelectedPaths();
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

function export_to_kml($id, $name, $desc){
  window.open("index.php?act=generate_path_kml_file&id="+$id+"&name="+$name+"&desc="+$desc, '_blank');
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
