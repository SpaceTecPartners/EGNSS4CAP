<?php
namespace login;

class login_view{

  protected $TEMPLATES_PATH;
  protected $JS_PATH;
  protected $CSS_PATH;
  private const PAGE_ID = 1;

  private const MY_PATH = 'login' . DIRECTORY_SEPARATOR;

  private $twig;
  public function __construct($path = ''){
    $this->setTwig(new \twig_controler());
    $this->setTEMPLATES_PATH(self::MY_PATH.$path);
    $this->setJS_PATH('js' . DIRECTORY_SEPARATOR . self::MY_PATH . $path);
    $this->setCSS_PATH('css' . DIRECTORY_SEPARATOR . self::MY_PATH . $path);
    $this->getTwig()->setPage_id(self::PAGE_ID);
  }

  public function load_login_html_page(array $twig_array){
    $twig = $this->getTwig();
    $twig->use_bootstrap_css();
    $twig->pushCss('css' . DIRECTORY_SEPARATOR . 'login' . DIRECTORY_SEPARATOR . 'login.css');
    $twig->pushCss($twig::GENERAL_CSS);
    $twig->pushCss($this->getCSS_PATH() . 'login.css');
    return $twig->render($this->getTEMPLATES_PATH() . 'login.html.twig', $twig_array, false, $_SESSION['lang'], self::PAGE_ID);
  }

  public function getTwig(): \twig_controler{
    return $this->twig;
  }

  public function setTwig($twig){
    $this->twig = $twig;
  }

  protected function getTEMPLATES_PATH(){
    return $this->TEMPLATES_PATH;
  }

  protected function setTEMPLATES_PATH($TEMPLATES_PATH){
    $this->TEMPLATES_PATH = $TEMPLATES_PATH;
  }

  protected function getJS_PATH(){
    return $this->JS_PATH;
  }

  protected function setJS_PATH($JS_PATH){
    $this->JS_PATH = $JS_PATH;
  }

  protected function getCSS_PATH(){
    return $this->CSS_PATH;
  }

  protected function setCSS_PATH($CSS_PATH){
    $this->CSS_PATH = $CSS_PATH;
  }
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
