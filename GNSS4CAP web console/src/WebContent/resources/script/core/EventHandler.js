cryptoFoto = cryptoFoto || {};

cryptoFoto.eventHandler = function(){
	
	this.handler = new Map();
	this.listeners = {};
	this.index = 1;
};

cryptoFoto.eventHandler.prototype.getHandler = function(){
	
	return this.handler;
};

cryptoFoto.eventHandler.prototype.setHandler = function(handler){
	
	this.handler = handler;
};


cryptoFoto.eventHandler.prototype.set = function(id, index){
	
	this.handler.set(id, index);
};

cryptoFoto.eventHandler.prototype.get = function(id){
	
	return this.handler.get(id);
};

cryptoFoto.eventHandler.prototype.addListener = function(element, event, handler, capture) {
	
    element.addEventListener(event, handler, capture);
    this.listeners[this.index] = {element: element, 
                     event: event, 
                     handler: handler, 
                     capture: capture};
    
    return (this.index)++;
};

cryptoFoto.eventHandler.prototype.removeListener = function(id) {
	

    if(id in this.listeners) {
        var h = this.listeners[id];
        h.element.removeEventListener(h.event, h.handler, h.capture);

        delete this.listeners[id];
    }
};