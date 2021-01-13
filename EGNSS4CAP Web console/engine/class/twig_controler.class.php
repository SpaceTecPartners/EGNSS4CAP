<?php

class twig_controler{
  private $twig_loader;
  private $twig;
  private $javascript;
  private $css;
  private $page_id;
  private const BASE_HTML = 6;

  public const GENERAL_CSS = 'css' . DIRECTORY_SEPARATOR . 'general.css';
  public const GENERAL_JS = 'js' . DIRECTORY_SEPARATOR . 'general.js';

  public function __construct(){
    $this->setTwig_loader(new Twig\Loader\FilesystemLoader(INCLUDE_PATH . 'templates'));
    $this->setTwig(new Twig\Environment($this->getTwig_loader()));
    $this->javascript = array();
    $this->css = array();
  }

  public function render(string $template, array $fields, $useBootstrao = true, $lang = 'en', $page_id = 0){
    $renderer = $this->getTwig();
    if($useBootstrao){
      $this->use_bootstrap_css();
      $this->use_bootstrap_js();
    }
    $this->pushCss(self::GENERAL_CSS);
    $this->pushJavascript(self::GENERAL_JS);
    $fields['javascript'] = $this->getJavascript();
    $fields['css'] = $this->getCss();
    $fields['app_version'] = APP_VERSION;
    $fields['user_name'] = isset($_SESSION['user_name'])? $_SESSION['user_name'] : "";
    $fields['map_api_key'] = 'AIzaSyARC8x_w7v84od0ZUz0WAKn5ECMN2inoRA';
    $fields['lang'] = $_SESSION['lang'];
    if($page_id){
      $fields = array_merge($fields, $this->load_lang($lang, $page_id));
    }
    return $renderer->render($template, $fields);
  }

  public function load_lang($lang, $page_id){
    $lang_texts = dibi::select('%sql', $lang)->as('text')
    ->select('template_param')
    ->from('page_lang')
    ->where('page_id = %i OR page_id = %i', self::BASE_HTML, $page_id)->fetchAll();
    $template_param_lang = array();
    foreach($lang_texts as $lang_text){
      $template_param_lang[$lang_text->template_param] = $lang_text->text;
    }
    return $template_param_lang;
  }

  public function getTwig_loader(){
    return $this->twig_loader;
  }
  public function setTwig_loader($twig_loader){
    $this->twig_loader = $twig_loader;
  }

  public function use_jquery(){
    $this->pushJavascript('vendor' . DIRECTORY_SEPARATOR . 'components' . DIRECTORY_SEPARATOR . 'jquery' . DIRECTORY_SEPARATOR . 'jquery.min.js');
  }

  public function use_bootstrap_css(){
    $this->pushCss('css' . DIRECTORY_SEPARATOR . 'bootstrap' . DIRECTORY_SEPARATOR . 'bootstrap.css');
  }

  public function use_bootstrap_js(){
    $this->pushJavascript('js' . DIRECTORY_SEPARATOR . 'bootstrap' . DIRECTORY_SEPARATOR . 'bootstrap.min.js');
  }

  public function use_map_js(){
    $this->pushJavascript('js' . DIRECTORY_SEPARATOR . 'map.js');
    $this->pushCss('css' . DIRECTORY_SEPARATOR . 'map_popup.css');
  }

  public function getTwig(){
    return $this->twig;
  }
  public function setTwig($twig){
    $this->twig = $twig;
  }

  public function getJavascript(){
    return $this->javascript;
  }

  public function pushJavascript($javascript){
    $this->javascript[] = $javascript;
  }

  public function getCss(){
    return $this->css;
  }
  public function pushCss($css){
    $this->css[] = $css;
  }

  public function getPage_id(){
    return $this->page_id;
  }

  public function setPage_id($page_id){
    $_SESSION['page_id'] = $page_id;
    return $this;
  }
}

?>
