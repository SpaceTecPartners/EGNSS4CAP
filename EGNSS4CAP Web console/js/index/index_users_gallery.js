const FILTER_TYPE = 'users_gallery';
var map_property = {zoom: 4, popup_bool: true};
let modalId = $('#image-gallery');

$(document)
.ready(function () {
  loadGallery(true, 'label.thumbnail');

  //This function disables buttons when needed
  function disableButtons(counter_max, counter_current) {
    if (counter_max > 1){
      $('#show-previous-image, #show-next-image')
        .show();
      if (counter_max === counter_current) {
        $('#show-next-image')
          .hide();
      } else if (counter_current === 1) {
        $('#show-previous-image')
          .hide();
      }
    } else {
      $('#show-previous-image, #show-next-image')
        .hide();
    }
  }

  /**
   *
   * @param setIDs        Sets IDs when DOM is loaded. If using a PHP counter, set to false.
   * @param setClickAttr  Sets the attribute for the click handler.
   */

  function loadGallery(setIDs, setClickAttr) {
    let current_image,
      selector,
      counter = 0;

    $('#show-next-image, #show-previous-image')
      .click(function () {
        if ($(this)
          .attr('id') === 'show-previous-image') {
          current_image--;
        } else {
          current_image++;
        }

        selector = $('[data-image-id="' + current_image + '"]');
        updateGallery(selector);
      });

    function updateGallery(selector) {
      let $sel = selector;
      current_image = $sel.data('image-id');
      $('#image-gallery-title')
        .text($sel.data('title'));
      $('#image-gallery-image')
        .attr('src', $sel.data('image'))
        .css('transform', 'rotate('+$sel.data('rotation')+'deg)')
        .data('rotation', $sel.data('rotation'))
        .data('photo_db_id', $sel.data('photo_db_id'));
      $('#image-gallery-zoom')
       .attr('href', $sel.data('image'));
      disableButtons(counter, $sel.data('image-id'));
    }

    if (setIDs == true) {
      $('[data-image-id]')
        .each(function () {
          counter++;
          $(this)
            .attr('data-image-id', counter);
        });
    }
    $(setClickAttr)
      .on('click', function () {
        updateGallery($(this));
      });
  }
});

$(document)
.keydown(function (e) {
  switch (e.which) {
    case 37: // left
      if ((modalId.data('bs.modal') || {})._isShown && $('#show-previous-image').is(":visible")) {
        $('#show-previous-image')
          .click();
      }
      break;

    case 39: // right
      if ((modalId.data('bs.modal') || {})._isShown && $('#show-next-image').is(":visible")) {
        $('#show-next-image')
          .click();
      }
      break;

    default:
      return; // exit this handler for other keys
  }
  e.preventDefault(); // prevent the default action (scroll / move caret)
})
.on('click', '.js_photo_rotate_left', async function(){
  $photo_id = $(this).data('pht_id');
  $photo_db_id = $("#"+$photo_id).data('photo_db_id');
  $rotation = parseInt($("#"+$photo_id).data('rotation')) - 90;
  await save_photo_rotation_to_db($photo_db_id,$rotation);
  $("#"+$photo_id).css('transform', 'rotate('+$rotation+'deg)');
  if ($photo_id == 'image-gallery-image'){
    $("#photo_"+$photo_db_id).css('transform', 'rotate('+$rotation+'deg)');
    $("#photo_"+$photo_db_id).data('rotation', $rotation);
  }
  $("#"+$photo_id).data('rotation', $rotation);
})
.on('click', '.js_photo_rotate_right', async function(){
  $photo_id = $(this).data('pht_id');
  $photo_db_id = $("#"+$photo_id).data('photo_db_id');
  $rotation = parseInt($("#"+$photo_id).data('rotation')) + 90;
  await save_photo_rotation_to_db($photo_db_id,$rotation);
  $("#"+$photo_id).css('transform', 'rotate('+$rotation+'deg)');
  if ($photo_id == 'image-gallery-image'){
    $("#photo_"+$photo_db_id).css('transform', 'rotate('+$rotation+'deg)');
    $("#photo_"+$photo_db_id).data('rotation', $rotation);
  }
  $("#"+$photo_id).data('rotation', $rotation);
})
.on('click', '#image-gallery-zoom', function(e){
  e.preventDefault();
  $rotation = $(this).find('img').data('rotation');
  $path = $(this).find('img').attr('src');
  window.open('photo_detail.php?rotation='+$rotation+'&img='+encodeURI($path), '_blank');
})
.on('click', '.js_select_all_photos', function(){
  $('.assign_photo_input').prop('checked', true);
})
.on('click', '.js_deselect_all_photos', function(){
  $('.assign_photo_input').prop('checked', false);
})
.on('click', '.js_photo_delete', async function(){
  await get_translate('photo_delete_confirm');
  if (confirm(alert_text)){
    delete_photo($(this).data('photo_db_id'));
  }
})
.on('click', '.js_photo_multi_delete', async function(){
  if ($('.assign_photo_input:checked').length > 0){
    await get_translate('photo_multi_delete_confirm');
    if (confirm(alert_text)){
      $.each($('.assign_photo_input:checked'), function(index, input){
        $reload = false;
        if ($('.assign_photo_input:checked').length == (index + 1)){
          $reload = true;
        }
        delete_photo($(input).val(), $reload);
      });
    }
  } else {
    await get_translate('assign_photos_select_error');
    alert(alert_text);
  }
})
.on('click', '.js_button_confirm_task_select', async function(e){
  e.preventDefault();
  if ($('.assign_photo_input:checked').length > 0 && $('#js_assign_photo_task_id_select').val() !== null){
    await get_translate('photo_assign_confirm');
    if (confirm(alert_text)){
      $("#assign_photos_form").submit();
    }
  } else if ($('#js_assign_photo_task_id_select').val() === null) {
    await get_translate('assign_photos_select_error_1');
    alert(alert_text);
  } else {
    await get_translate('assign_photos_select_error');
    alert(alert_text);
  }
})
.on('click', '.js_photo_select', function(){
  $elem = $("#" + $(this).data('checkbox_id'));
  if ($elem.is(":checked")){
    $elem.prop('checked', false);
  } else {
    $elem.prop('checked', true);
  }
})
.on('click', '.assign_photo_input', function(e){
  e.preventDefault();
})
.on('click', '.js_button_open_task_select', async function(){
  await get_translate('assign_photos_select_error');
  if ($('.assign_photo_input:checked').length > 0){
    $("#assign_photos_form").submit();
  } else {
    alert(alert_text);
  }
})


function delete_photo($id, $reload = true){
  $.ajax({
    type: "post",
    url: "index.php",
    data: {
      act: 'delete_photo',
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

function save_photo_rotation_to_db($photo_db_id, $rotation){
  return new Promise(options =>{
    options(
      $.ajax({
        type: "post",
        url: "index.php",
        data: {
          act: 'save_photo_rotation',
          photo_id: $photo_db_id,
          rotation: $rotation
        },
        dataType: "JSON",
        success: function (res) {
          alert_text = res;
        }
      })
    )
  });
}
