(function() {
	var defaults;
	var apps = {};
	var themesLoaded = {};
	var widgetsets = {};
	
	
    var log;
    if (typeof console === "undefined") {
    	//If no console.log present, just use a no-op
    	log = function() {};
    } else if (typeof console.log === "function") {
    	//If it's a function, use it with apply
		log = function() {
			console.log.apply(console, arguments);
		};
    } else {
    	//In IE, its a native function for which apply is not defined, but it works without a proper 'this' reference
    	log = console.log;
    }
	
	var loadTheme = function(url) {
		if(!themesLoaded[url]) {
			log("loadTheme", url);
			var stylesheet = document.createElement('link');
			stylesheet.setAttribute('rel', 'stylesheet');
			stylesheet.setAttribute('type', 'text/css');
			stylesheet.setAttribute('href', url + "/styles.css");
			document.getElementsByTagName('head')[0].appendChild(stylesheet);
			themesLoaded[url] = true;
		}		
	}
	
	var isWidgetsetLoaded = function(widgetset) {
		var className = widgetset.replace(/\./g, "_");
		return (typeof window[className]) != "undefined";
	}
	
	var loadWidgetset = function(basePath, widgetset) {
		if (widgetsets[widgetset]) {
			return;
		}
		log("load widgetset", basePath, widgetset)
		setTimeout(function() {
			if (!isWidgetsetLoaded(widgetset)) {
				alert("Failed to load the widgetset: " + url);
			}
		}, 15000);
		
		var url = basePath + widgetset + "/" + widgetset + ".nocache.js?" + new Date().getTime();
		
		var scriptTag = document.createElement('script');
		scriptTag.setAttribute('type', 'text/javascript');
		scriptTag.setAttribute('src', url);
		document.getElementsByTagName('head')[0].appendChild(scriptTag);
		
		widgetsets[widgetset] = {
			pendingApps: []
		};
	}
	
	window.vaadin = window.vaadin || {
		setDefaults: function(d) {
			if (defaults) {
				throw "Defaults already defined";
			}
			log("Got defaults", d)
			defaults = d;
		},
		initApplication: function(appId, config) {
			if (apps[appId]) {
				throw "Application " + appId + " already initialized";
			}
			log("init application", appId, config);
			var getConfig = function(name) {
				var value = config[name];
				if (value === undefined) {
					value = defaults[name];
				}
				return value;
			}
			
			var fetchRootConfig = function() {
				log('Fetching root config');
				var url = getConfig('appUri');
				// Root id
				url += ((/\?/).test(url) ? "&" : "?") + "browserDetails";
				url += '&rootId=' + getConfig('rootId');
				// Uri fragment
				url += '&f=' + encodeURIComponent(location.hash);
				// Timestamp to avoid caching
				url += '&' + (new Date()).getTime();
				
				var r = new XMLHttpRequest();
				r.open('POST', url, true);
				r.onreadystatechange = function (aEvt) {  
					if (r.readyState == 4) {
						if (r.status == 200){
							log("Got root config response", r.responseText);
							// TODO Does this work in all supported browsers?
							var updatedConfig = JSON.parse(r.responseText);
							
							// Copy new properties to the config object
							for (var property in updatedConfig) {
								if (updatedConfig.hasOwnProperty(property)) {
									config[property] = updatedConfig[property];
								}
							}
							config.initPending = false;
							
							// Try bootstrapping again, this time without fetching missing info
							bootstrapApp(false);
						} else {
							log('Error', r.statusText);  
						}
					}  
				};
				r.send(null);
				
				log('sending request to ', url);
			};			
			
			//Export public data
			var app = {
				'getConfig': getConfig
			};
			apps[appId] = app;
			
			var bootstrapApp = function(mayDefer) {
				var themeUri = getConfig('themeUri');
				if (themeUri) {
					loadTheme(themeUri);
				}
				
				var widgetsetBase = getConfig('widgetsetBase');
				var widgetset = getConfig('widgetset');
				var initPending = getConfig('initPending');
				if (widgetset && widgetsetBase) {
					loadWidgetset(widgetsetBase, widgetset);
				}
				
				if (initPending) {
					if (mayDefer) {
						fetchRootConfig();
					} else {
						throw "May not defer bootstrap any more";
					}
				} else {
					if (widgetsets[widgetset].callback) {
						log("Starting from bootstrap", appId);
						widgetsets[widgetset].callback(appId);
					}  else {
						log("Setting pending startup", appId);
						widgetsets[widgetset].pendingApps.push(appId);
					}
				}
			}
			bootstrapApp(true);

			if (getConfig("debug")) {
				// TODO debug state is now global for the entire page, but should somehow only be set for the current application  
				window.vaadin.debug = true;
			}
			
			return app;
		},
		getApp: function(appId) {
			var app = apps[appId]; 
			return app;
		},
		registerWidgetset: function(widgetset, callback) {
			log("Widgetset registered", widgetset)
			widgetsets[widgetset].callback = callback;
			for(var i = 0; i < widgetsets[widgetset].pendingApps.length; i++) {
				var appId = widgetsets[widgetset].pendingApps[i];
				log("Starting from register widgetset", appId);
				callback(appId);
			}
			widgetsets[widgetset].pendingApps = null;
		}
	};
	
	log('Vaadin bootstrap loaded');
})();