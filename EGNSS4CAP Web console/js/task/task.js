const FILTER_TYPE = 'task_detail';
let modalId = $('#image-gallery');
var map_property = {zoom: 17, popup_bool: true};
var alert_text, db_data;

$(document)
  .ready(function () {
    loadGallery(true, 'a.thumbnail');

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
          .css('transform', 'rotate('+$sel.find('.img-thumbnail').data('rotation')+'deg)')
          .data('rotation', $sel.find('.img-thumbnail').data('rotation'))
          .data('photo_db_id', $sel.find('.img-thumbnail').data('photo_db_id'));
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

// build key actions
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
  .on('click', '.js_ack', async function(){
    await get_translate('task_accept_confirm');
    if (confirm(alert_text)){
      accept_task();
    }
  })
  .on('click', '.js_decline', async function(){
    await get_translate('task_decline_confirm');
    $text_ret = prompt(alert_text, "");
    if ($text_ret != null){
      decline_task($text_ret);
    }
  })
  .on('click', '.js_return', async function(){
    await get_translate('task_return_confirm');
    $text_ret = prompt(alert_text, "");
    if ($text_ret != null){
      return_task($text_ret);
    }
  })
  .on('click', '.js_delete', async function(){
    await get_translate('task_delete_confirm');
    if (confirm(alert_text)){
      delete_task();
    }
  })
  .on('click', '.js_move_from_open', async function(){
    await get_translate('task_move_from_open_confirm');
    await get_data_from_db('task_note', $('.js_task_id').attr('data-href'));
    $text_note = prompt(alert_text, db_data);
    if ($text_note != null){
      move_from_open_task($text_note);
    }
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

  function accept_task(){
    $.ajax({
      type: "post",
      url: "task.php",
      data: {
        act: 'accept_task',
        id: $('.js_task_id').attr('data-href')
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
  function decline_task($text_ret=""){
    $.ajax({
      type: "post",
      url: "task.php",
      data: {
        act: 'decline_task',
        id: $('.js_task_id').attr('data-href'),
        text: $text_ret
      },
    }).done(function(){
      location.reload();
    })
  }
  function return_task($text_ret=""){
    $.ajax({
      type: "post",
      url: "task.php",
      data: {
        act: 'return_task',
        id: $('.js_task_id').attr('data-href'),
        text: $text_ret
      },
    }).done(function(){
      location.reload();
    })
  }
  function delete_task(){
    $.ajax({
      type: "post",
      url: "task.php",
      data: {
        act: 'delete_task',
        id: $('.js_task_id').attr('data-href')
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
  function move_from_open_task($text_note=""){
    $.ajax({
      type: "post",
      url: "task.php",
      data: {
        act: 'move_from_open_task',
        id: $('.js_task_id').attr('data-href'),
        text: $text_note
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
  function get_translate($tmp_param){
    return new Promise(options =>{
      options(
        $.ajax({
          type: "post",
          url: "index.php",
          data: {
            act: 'get_translate',
            tmp_param: $tmp_param
          },
          dataType: "JSON",
          success: function (res) {
            alert_text = res;
          }
        })
      )
    });
  }

  function get_data_from_db($type, $id){
    return new Promise(options =>{
      options(
        $.ajax({
          type: "post",
          url: "index.php",
          data: {
            act: 'get_data_from_db',
            type: $type,
            id: $id
          },
          dataType: "JSON",
          success: function (res) {
            db_data = res;
          }
        })
      )
    });
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
