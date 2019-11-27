function Map() {
	this.keys = new Array();
	this.data = new Object();
	this.size = 0;
	
	this.getKeys = function(){
		
		return this.keys;
	};
	this.getFirstKey = function(){
		
		return this.keys[0];
	};
	
	this.has = function(key){
		
		if($.inArray(key, this.keys) >= 0)
			return true;
		else
			return false;
	};
	
	this.set = function(key, value) {
	    
		if(this.data[key] == null){
	        this.keys.push(key);
		}
		
	    this.data[key] = value;
	    this.size = this.keys.length;
	};
	
	this.get = function(key) {
	    
		return this.data[key];
	};
	
	this.forEach = function(f, self) {

		try {
			if(this.keys.length > 1){
	        	
				this.keys = this.keys.sort();
	        }
			for (var i = 0;i <  this.keys.length; i++) {
				var key = this.keys[i];
				var value = this.data[key];
				if (value)
					f.call(self, value, key);
			}
		} finally {
			//this.maybeCleanup();
		}
	};
	
	this.clear = function() {
		
		this.keys = new Array();
		this.data = new Object();
		this.size = 0;
	};
	
	this.remove = function(key) {

		var index = this.keys.indexOf(key);
		if (index > -1) {
			
		    this.keys.splice(index,1);
		}
		
	    this.data[key] = null;
	    this.size--;
	};
	
	this.each = function(fn){
	    if(typeof fn != 'function'){
	        return;
	    }
	    var len = this.keys.length;
	    for(var i=0;i<len;i++){
	        var k = this.keys[i];
	        fn(k,this.data[k],i);
	    }
	};
	
	this.entrys = function() {
	    var len = this.keys.length;
	    var entrys = new Array(len);
	    for (var i = 0; i < len; i++) {
	        entrys[i] = {
	            key : this.keys[i],
	            value : this.data[i]
	        };
	    }
	    return entrys;
	};
	
	this.isEmpty = function() {
	    return this.keys.length == 0;
	};
	
}
