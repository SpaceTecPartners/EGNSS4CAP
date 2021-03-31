var alert_text, db_data;

$(document)
  .on('click', '.js_farmer_tasks', function() {
    var id = $(this).attr("data-href");
    window.location.href = "index.php?act=agency_user_tasks_list&id=" + id;
  })
  .on('click', '.js_new_farmer_show', function(){
    show_hide_new_farmers_inputs(this);
  })
  .on('click', '.js_button_open_user_edit', function(){
    show_hide_new_farmers_inputs(this);
  })
  .on('click', '.js_button_close_farmer_edit', function(){
    hide_new_farmers_inputs(this);
  })
  .on('click', '.clickGroupTaskAccept', async function(){
    await get_translate('group_task_accept_confirm');
    if (confirm(alert_text)){
      accept_group_tasks();
    }
  })
  /*.on('click', '.js_new_farmer', function(){
    save_new_farmer();
  })*/
  .on('click', '.js_new_task_show', function(){
    show_hide_new_task_inputs(this);
  })
  /*.on('click', '.js_new_task', function(){
    save_new_task()
  })*/
  .on('click', '.js_task_detail', function(){
    var id = $(this).attr("data-href");
    window.location.href = "task.php?act=task_detail&id=" + id;
  })

  .on('submit', '#new_task_form', function(e){
    e.preventDefault();
    save_new_task();
  })
  .on('click', '#unassigned_pdf_export', async function(e){
    e.preventDefault();
    var url = $(this).attr('href');
    await get_translate('assign_photos_select_error');
    if ($('#assign_photos_form').find('.assign_photo_input').length > 0){
      $photos = $('#assign_photos_form').find('.assign_photo_input');
      var specified_photos = '&specified_photos=';
      var first = true;
      $.each($photos, function () {
        if (first){
          specified_photos += $(this).val();
          first = false;
        } else {
          specified_photos += ','+$(this).val();
        }
      });
      window.open(url + specified_photos, '_blank');
    } else {
      alert(alert_text);
    }
    //window.open(url, '_blank');
  })
  .on('click', '#unassigned_pdf_export_selected', async function(e){
    e.preventDefault();
    var url = $(this).attr('href');
    await get_translate('assign_photos_select_error');
    if ($('#assign_photos_form').find('.assign_photo_input:checked').length > 0){
      $photos = $('#assign_photos_form').find('.assign_photo_input:checked');
      var specified_photos = '&specified_photos=';
      var first = true;
      $.each($photos, function () {
        if (first){
          specified_photos += $(this).val();
          first = false;
        } else {
          specified_photos += ','+$(this).val();
        }
      });
      window.open(url + specified_photos, '_blank');
    } else {
      alert(alert_text);
    }
  })


  function save_new_task(){
    var form  = $('#new_task_form');
    var data = new FormData(form[0]);
    data.append('act', 'add_task');
    $.ajax({
      type: "POST",
      url: "index.php",
      async: true,
      processData: false,
      contentType: false,
      data: data,
    }).done(function(result){
      data = JSON.parse(result);
      if (data.error=='1'){
        alert(data.errorText);
      } else {
        location.reload();
      }
    })
  }
  function accept_task($id = 0, $reload = false){
    $.ajax({
      type: "post",
      url: "task.php",
      data: {
        act: 'accept_task',
        id: $id,
        reload: $reload
      },
    }).done(function(result){
      data = JSON.parse(result);
      if (data.reload=='true'){
        location.reload();
      }
    })
  }
  function accept_group_tasks(){
    var tasks = getCheckedBoxes("js_group_task_accept_chck");
    for (i = 0; i < tasks.length; i++) {
      $reload = false;
      $id = $(tasks[i]).data('task_id');
      if (i == (tasks.length - 1)) { $reload = true; }
      accept_task($id, $reload);
    }
  }
  function show_hide_new_task_inputs ($button){
    if($('.js_new_task_inputs').is(":hidden")){
      $('.js_new_task_inputs').show();
      $($button).hide();
    }else{
      $('.js_new_task_inputs').hide();
    }
  }

  function show_hide_new_farmers_inputs ($button){
    $form = $('.js_new_farmer_inputs');
    scrollToHeading('#js_fi_header');
    if($form.is(":hidden")){
      clear_form($form);
      $form.show();
      if ($($button).hasClass('js_new_farmer_show')){
        $($button).hide();
        fill_user_form($($button), $form);
      } else if ($($button).hasClass('js_button_open_user_edit')) {
        $('.js_new_farmer_show').hide();
        fill_user_form($($button), $form);
      }
    }else{
      clear_form($('.js_new_farmer_inputs'));
      $('.js_new_farmer_show').hide();
      fill_user_form($($button), $form);
    }
  }

  function hide_new_farmers_inputs (){
    $('.js_new_farmer_inputs').hide();
    $('.js_new_farmer_show').show();
  }

  function getCheckedBoxes(chkboxClassName) {
    var checkboxes = document.getElementsByClassName(chkboxClassName);
    var checkboxesChecked = [];
    for (var i=0; i<checkboxes.length; i++) {
       if (checkboxes[i].checked) {
          checkboxesChecked.push(checkboxes[i]);
       }
    }
    return checkboxesChecked.length > 0 ? checkboxesChecked : null;
  }

  function clear_form($form){
    $form.find('input:not(.not_clear)').val("");
  }

  function fill_user_form($button, $form){
    $uid = $button.data('userid');
    $header = $button.data('formheader');
    $login = $button.data('userlogin');
    $name = $button.data('username');
    $surname = $button.data('usersurname');
    $email = $button.data('useremail');
    $in = $button.data('userin');
    $vat = $button.data('uservat');
    $action = "new_farmer";
    if($uid !== "0" && $uid !== 0){
      $action = "edit_farmer";
      $form.find("#js_fi_pass").removeAttr('required');
      $form.find("#js_fi_login").removeAttr('required');
      $form.find("#js_fi_login").attr('disabled', 'disabled');
      $form.find("#js_fi_uid").val($uid);
      $form.find("#js_fi_login").val($login);
      $form.find("#js_fi_name").val($name);
      $form.find("#js_fi_surname").val($surname);
      $form.find("#js_fi_email").val($email);
      $form.find("#js_fi_in").val($in);
      $form.find("#js_fi_vat").val($vat);
    } else {
      $form.find("#js_fi_pass").attr('required', 'required');
      $form.find("#js_fi_login").attr('required', 'required');
      $form.find("#js_fi_login").removeAttr('disabled');
    }
    $form.find("#js_fi_header").html($header);
    $form.find("#js_fi_act").val($action);
  }

  function show_hide_task_select($button){
    if($('.js_assign_photos_task_select').is(":hidden")){
      $('.js_assign_photos_task_select').show();
      $($button).hide();
    }else{
      $('.js_assign_photos_task_select').hide();
    }
  }


  function save_new_farmer(){
    var form  = $('#new_farmer_form');
    var data = new FormData(form[0]);
    data.append('act', 'new_farmer');
    $.ajax({
      type: "POST",
      url: "index.php",
      async: true,
      processData: false,
      contentType: false,
      data: data,
    }).done(function(){
      location.reload();
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

  function get_data_from_db($type){
    return new Promise(options =>{
      options(
        $.ajax({
          type: "post",
          url: "index.php",
          data: {
            act: 'get_data_from_db',
            type: $type
          },
          dataType: "JSON",
          success: function (res) {
            db_data = res;
          }
        })
      )
    });
  }

  function scrollToTop($offset=0){
    var body = $("html, body, main");
    body.stop().animate({scrollTop:$offset}, 500, 'swing', function() {});
  }

  function scrollToHeading(target) {
    setTimeout(
      function() {
        $('html, body, main').animate({scrollTop: 0},0);//otherwise the target position is wrong, due to the fact we're not scrolling body, but main
        $('html, body, main').animate({scrollTop: (($(target).position().top)-70)},500);
      }, 10);
  }
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
