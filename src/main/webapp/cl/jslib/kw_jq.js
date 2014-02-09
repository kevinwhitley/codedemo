
function getUrlParameter (url, param)
{
    // strip the #blahblah off the end of the url, so the last parameter isn't junk
    var strippedUrl = url;
    var indx = url.indexOf('#');
    if (indx >= 0) {
        strippedUrl = url.substring(0, indx);
    }
    var indx = strippedUrl.indexOf(param+"=");
    var result = null;
    if (indx >= 0) {
        indx += param.length + 1;
        var end = strippedUrl.indexOf('&', indx+1);
        if (end >= 0) {
            result = strippedUrl.substring(indx, end);
        }
        else {
            result = strippedUrl.substring(indx);
        }
    }
    return result;
}

/*
function tester()
{
    testreplace(1, '/photo/xx?name=foo', 'name', 'bar', '/photo/xx?name=bar');
    testreplace(2, '/photo/xx?name=hoho', 'image', 'first', '/photo/xx?name=hoho&image=first');
    testreplace(3, '/photo/xx?name=hoho&image=first&tester=joe', 'image', null, '/photo/xx?name=hoho&tester=joe');
    testreplace(4, '/photo/xx?name=hoho&image=first', 'name', null, '/photo/xx?image=first');
    testreplace(5, '/photo/xx?name=hoho&image=first', 'image', null, '/photo/xx?name=hoho');
    testreplace(6, '/photo/xx?single=yes', 'single', null, '/photo/xx');
    testreplace(7, '/photo/xx?double=xx&frost=blah', 'double', 'zz', '/photo/xx?double=zz&frost=blah');
    testreplace(8, '/photo/xx', 'p', '99', '/photo/xx?p=99');
    testreplace(9, '/photo/xx?hey=ho', 'non', null, '/photo/xx?hey=ho');

    testreplace(11, '/photo/xx?name=foo#hasher', 'name', 'bar', '/photo/xx?name=bar#hasher');
    testreplace(12, '/photo/xx?name=hoho#hasher', 'image', 'first', '/photo/xx?name=hoho&image=first#hasher');
    testreplace(13, '/photo/xx?name=hoho&image=first&tester=joe#hasher', 'image', null, '/photo/xx?name=hoho&tester=joe#hasher');
    testreplace(14, '/photo/xx?name=hoho&image=first#hasher', 'name', null, '/photo/xx?image=first#hasher');
    testreplace(15, '/photo/xx?name=hoho&image=first#hasher', 'image', null, '/photo/xx?name=hoho#hasher');
    testreplace(16, '/photo/xx?single=yes#hasher', 'single', null, '/photo/xx#hasher');
    testreplace(17, '/photo/xx?double=xx&frost=blah#hasher', 'double', 'zz', '/photo/xx?double=zz&frost=blah#hasher');
    testreplace(18, '/photo/xx#zz', 'p', '99', '/photo/xx?p=99#zz');
    testreplace(19, '/photo/xx?hey=ho#zz', 'non', null, '/photo/xx?hey=ho#zz');
}

function testreplace(testIndex, url, parameter, value, expected)
{
    var urlNew = kw_replaceUrlParameter(url, parameter, value);
    if (urlNew == expected) {
        console.log('test ' + testIndex + ' passed');
    }
    else {
        console.log('test ' + testIndex + ' failed');
        console.log('  expected: ' + expected);
        console.log('       got: ' + urlNew);
    }
}
*/

// replace a parameter within a url & return the modified url
// this method assumes that the parameter appears no more than once in the url (no multiple values)
// if the parameter doesn't exist - we will add it
// if the value is null, then the parameter (if it exists) will be removed
function kw_replaceUrlParameter(url, parameter, value)
{
    if (!url || url.length < 1) {
        return;
    }
    // simple string manipulation of the url
    
    // find the parameter
    var pStart = url.indexOf(parameter + '=');
    var pEnd = -1;
    if (pStart >= 0) {
        pEnd = url.indexOf('&', pStart);
        if (pEnd < 0) {
            pEnd = url.indexOf('#');
        }
        if (pEnd < 0) {
            pEnd = url.length;
        }
    }
    
    // now do a string replacement
    if (value) {
        if (pStart >= 0) {
            url = url.substring(0, pStart+parameter.length+1) + value + url.substring(pEnd);
        }
        else {
            // just tack the parameter on to the end
            var addChar = (url.indexOf('?') >= 0) ? '&' : '?';
            var hashIndex = url.indexOf('#');
            if (hashIndex >= 0) {
                url = url.substring(0, hashIndex) + addChar + parameter + '=' + value + url.substring(hashIndex);
            }
            else {
                url = url + addChar + parameter + '=' + value;
            }
        }
    }
    else {
        // we're just removing the parameter
        if (pStart >= 0) {
            if (pEnd < url.length && url.charAt(pEnd) === '&') {
                // parameter is in the middle of a list
                // extend pEnd to remove the following ampersand
                pEnd++;
            }
            else if (pEnd == url.length || url.charAt(pEnd === '#')) {
                // this is the last parameter - remove the character before the parameter
                pStart--;
            }
            url = url.substring(0, pStart) + url.substring(pEnd);
        }
    }
    
    return url;
}

// construct a url based on a location (/whatever) and parameters (in a map)
// this routine does the parameter escaping, and sorts parameters alphabetically
// also optionally adds a cache-breaking parameter
// returns the url string
function kw_constructUrl(location, parameters, breakCache)
{
    var url = location;
    var first = true;
    var parameterNames = [];
    for (var pp in parameters) {
        if (parameters.hasOwnProperty(pp)) {
            parameterNames.push(pp);
        }
    }
    parameterNames.sort();

    for (var ii=0; ii<parameterNames.length; ii++) {
        pp = parameterNames[ii];
        var val = parameters[pp];
        if (first) {
            url += '?';
            first = false;
        }
        else {
            url += '&';
        }
        url += pp + '=' + encodeURIComponent(val);
    }
    
    if (breakCache) {
        url += (parameterNames.length > 0) ? '&' : '?';
        url += 'zzcb=' + Math.floor(Math.random()*10000);
    }

    return url;
}

function kw_ajaxForm(formId, responseFunction)
{
    var jForm = $('#'+formId);
    kw_ajaxPost(jForm.attr('action'), jForm.serialize(), responseFunction);
}

function kw_ajaxGet(url, data, responseFunction, errorFunction)
{
    var standardActions = function(response)
    {
        //alert("kw_ajaxGet " + response.action);
        if (response.action == 'redirect') {
            window.location = response.url;
        }
        else if (response.action == 'json') {
            responseFunction(response.data);
        }
        else if (responseFunction) {
            responseFunction(response);
        }
    }
    
    var options = {
        method: 'GET',
        dataType: 'json',
        data: data,
        success: standardActions,
        error: errorFunction || null
    };
    //alert("calling ajax " + url);
    $.ajax(url, options);
}

function kw_ajaxGetRawJson(url, data, responseFunction, errorFunction)
{
    var options = {
        method: 'GET',
        dataType: 'json',
        data: data,
        success: responseFunction,
        error: errorFunction || null
    };
    $.ajax(url, options);
}

function kw_ajaxPost(url, parameters, responseFunction)
{
    var standardActions = function(response)
    {
        if (response.action == 'redirect') {
            window.location = response.url;
        }
        else if (response.action == 'json') {
            responseFunction(response.data);
        }
        else {
            responseFunction(response);
        }
    }

    var onFailure = function(xmlhttp, status, errorThrown)
    {
        alert("kw_ajaxPost failure! " + status);
        if (errorThrown) {
            alert("exception: " + errorThrown);
        }
    };

    var options = {
        type: 'POST',
        dataType: 'json',
        data: parameters,
        success: standardActions,
        error: onFailure
    };
    $.ajax(url, options);
}

function kw_readCookie(cname)
{
    var allcookies = document.cookie;
    var start = allcookies.indexOf(cname+"=");
    if (start < 0) {
        return null;
    }
    start += cname.length + 1;
    var end = allcookies.indexOf(";", start);
    var cookieString = (end < 0) ? allcookies.substring(start) : allcookies.substring(start,end);
    if (cookieString.charAt(0) != 'a') {
        // not our encoding version
        return null;
    }
    cookieString = decodeURIComponent(cookieString.substring(1));

    var result = {};
    var tokens = cookieString.split(";");
    for (var ii=0; ii<tokens.length; ii++) {
        var indx = tokens[ii].indexOf("=");
        if (indx < 1) {
            continue;
        }
        var key = tokens[ii].substring(0, indx);
        var val = tokens[ii].substring(indx+1);
        result[key] = val;
    }

    return result;
}

function kw_setCookie(name, value, seconds)
{
    var cookieValue = "a"; // our version mark
    for (var prop in value) {
        cookieValue += prop + "=" + value[prop] + ";";
    }
    cookieValue = encodeURIComponent(cookieValue);

    var expire = "";
    if (seconds) {
        var date = new Date();
        date.setTime(date.getTime() + seconds*1000);
        expire = "; expires="+date.toGMTString();
    }

    var wr = name+"="+cookieValue+expire+"; path=/";
    document.cookie = wr;
}

// trivial templating engine
function KwTemplate(template)
{
    this.template = template;
}

KwTemplate.prototype.evaluate = function(values)
{
    var result = this.template;
    var key;
    for (key in values) {
        if (values.hasOwnProperty(key)) {
            var expr = new RegExp("#{"+key+"}", "g");
            result = result.replace(expr, values[key]);
        }
    }

    return result;
};

// stop an event - either a jQuery normalized event, or an event from a DOM element
function kwstop(event)
{
    if (event) {
        if (event.originalEvent || event.preventDefault) {
            // this is a jQuery event object or standard DOM event object
            event.preventDefault();
            event.stopPropagation();
        }
        else {
            // a native IE event object
            event.cancelBubble = true;
            event.returnValue = false;
        }
    }
}

// get the position of a mouse event, relative to an element
// event is assumed to be a jQuery normalized event
// element is something we can wrap jQuery around
function kwmousePosition(event, element)
{
    var offset = jQuery(element).offset();
    return {left: event.pageX - offset.left, top: event.pageY - offset.top};
}

function kw_handleResize(namespace, handler)
{
    var kwResizeTimer = null;
    $(window).on('resize.kwresize-' + namespace, function() {
        if (kwResizeTimer) {
            window.clearTimeout(kwResizeTimer);
            kwResizeTimer = null;
        }
        kwResizeTimer = window.setTimeout(function() {
            kwResizeTimer = null;
            handler();
        }, 200);
    })
}

function kw_handleResizeRemove(namespace)
{
    $(window).off('resize.kwresize-' + namespace);
}

function kwidentify(domelement)
{
    var id = $(domelement).attr('id');
    if (!id) {
        id = 'autoId'+ (kwidentify.counter++);
        $(domelement).attr('id', id);
    }
    return id;
}
kwidentify.counter = 0;


function kwel(id)
{
    return document.getElementById(id);
}

