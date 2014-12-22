function CheckboxControl(str, def, callback) {
  var controlDiv = document.createElement('div');
  controlDiv.style.padding = '5px';

  // Set CSS for the control border.
  var controlUI = document.createElement('div');
  $(controlUI).css({'direction': 'ltr', 'overflow': 'hidden', 'text-align': 'left', 'position': 'relative', 'color': 'rgb(86, 86, 86)', 'font-family': 'Roboto,Arial,sans-serif', '-moz-user-select': 'none', 'font-size': '11px', 'background-color': 'rgb(255, 255, 255)', 'padding': '1px 4px 1px 1px', 'border-radius': '2px', 'background-clip': 'padding-box', 'border-width': '1px', 'border-style': 'solid', 'border-color': 'rgba(0, 0, 0, 0.15)', 'box-shadow': '0px 1px 4px -1px rgba(0, 0, 0, 0.3)'});
  controlDiv.appendChild(controlUI);
  
  var label = document.createElement('label');
  var text = document.createTextNode(str);

  var input = document.createElement('input');
  $(input).prop({'type': 'checkbox', 'checked': def});
  input.style.verticalAlign = 'middle';
  google.maps.event.addDomListener(input, 'change', function ()
  {
    callback($(input).prop('checked'));
  });

  label.appendChild(input);
  label.appendChild(text);
  controlUI.appendChild(label);

  this.getDiv = function ()
  {
    return controlDiv;
  }

  this.setText = function (str)
  {
    text.innerHTML = str;
  }
}

