package org.bhargav.pansariwala.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.map_picker_confirm
import pansariwala.shared.generated.resources.map_picker_pin_hint
import pansariwala.shared.generated.resources.map_picker_search_hint
import pansariwala.shared.generated.resources.map_picker_title
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual val mapLocationPickerSupported: Boolean = true

actual suspend fun pickLocationOnMap(lat: Double, lng: Double, zoom: Int): MapPickedLocation? {
    val title = getString(Res.string.map_picker_title)
    val searchHint = getString(Res.string.map_picker_search_hint)
    val pinHint = getString(Res.string.map_picker_pin_hint)
    val confirm = getString(Res.string.map_picker_confirm)
    val cancel = getString(Res.string.action_cancel)
    return suspendCancellableCoroutine { cont ->
        val handle = openMapPicker(
            AppConstants.GOOGLE_MAPS_API_KEY,
            AppConstants.PHOTON_SEARCH_URL,
            title,
            searchHint,
            pinHint,
            confirm,
            cancel,
            lat,
            lng,
            zoom,
            onPicked = { pLat, pLng, address, city, state, zip ->
                if (cont.isActive) cont.resume(MapPickedLocation(pLat, pLng, address, city, state, zip))
            },
            onCancel = { if (cont.isActive) cont.resume(null) },
            onError = { if (cont.isActive) cont.resumeWithException(MapPickerUnavailableException()) },
        )
        cont.invokeOnCancellation { closeMapPicker(handle) }
    }
}

private fun closeMapPicker(handle: JsAny): Unit = js("handle.close()")

/**
 * Google Maps JS overlay: the map stays under a fixed centre pin, search uses Places (new API, then the
 * legacy AutocompleteService, then geocoding, then Photon when Google refuses, e.g. billing off), and the
 * pinned point is reverse geocoded on idle.
 */
private fun openMapPicker(
    apiKey: String,
    photonUrl: String,
    title: String,
    searchHint: String,
    pinHint: String,
    confirmLabel: String,
    cancelLabel: String,
    lat: Double,
    lng: Double,
    zoom: Int,
    onPicked: (Double, Double, String, String, String, String) -> Unit,
    onCancel: () -> Unit,
    onError: () -> Unit,
): JsAny = js(
    """(() => {
  const handle = {};
  const overlay = document.createElement('div');
  overlay.style.cssText = 'position:fixed;inset:0;z-index:2147483000;background:rgba(0,0,0,.55);display:flex;align-items:center;justify-content:center;font-family:sans-serif;';
  const card = document.createElement('div');
  card.style.cssText = 'background:#fff;color:#1b1b1b;border-radius:16px;width:min(960px,96vw);height:min(760px,94vh);display:flex;flex-direction:column;overflow:hidden;';
  const head = document.createElement('div');
  head.style.cssText = 'padding:16px 16px 10px;display:flex;flex-direction:column;gap:8px;';
  const heading = document.createElement('div');
  heading.textContent = title;
  heading.style.cssText = 'font-size:18px;font-weight:600;';
  const searchBox = document.createElement('div');
  searchBox.style.cssText = 'position:relative;';
  const input = document.createElement('input');
  input.type = 'search';
  input.placeholder = searchHint;
  input.autocomplete = 'off';
  input.style.cssText = 'width:100%;box-sizing:border-box;padding:12px 14px;border:1px solid #c4c7c5;border-radius:10px;font-size:15px;outline:none;';
  const list = document.createElement('div');
  list.style.cssText = 'position:absolute;left:0;right:0;top:calc(100% + 4px);background:#fff;border-radius:10px;box-shadow:0 6px 20px rgba(0,0,0,.25);z-index:5;max-height:280px;overflow:auto;display:none;';
  searchBox.append(input, list);
  const help = document.createElement('div');
  help.textContent = pinHint;
  help.style.cssText = 'font-size:13px;color:#5f6368;';
  head.append(heading, searchBox, help);
  const mapWrap = document.createElement('div');
  mapWrap.style.cssText = 'position:relative;flex:1;min-height:0;background:#e8eaed;';
  const mapEl = document.createElement('div');
  mapEl.style.cssText = 'position:absolute;inset:0;';
  const pin = document.createElement('div');
  pin.textContent = '\uD83D\uDCCD';
  pin.style.cssText = 'position:absolute;left:50%;top:50%;transform:translate(-50%,-100%);font-size:40px;line-height:1;pointer-events:none;z-index:2;';
  mapWrap.append(mapEl, pin);
  const foot = document.createElement('div');
  foot.style.cssText = 'padding:12px 16px;display:flex;gap:12px;align-items:center;border-top:1px solid #e0e0e0;flex-wrap:wrap;';
  const addr = document.createElement('div');
  addr.style.cssText = 'flex:1 1 240px;font-size:13px;color:#3c4043;min-height:18px;';
  const cancelBtn = document.createElement('button');
  cancelBtn.textContent = cancelLabel;
  cancelBtn.style.cssText = 'padding:10px 18px;border-radius:20px;border:1px solid #c4c7c5;background:#fff;font-size:14px;cursor:pointer;';
  const okBtn = document.createElement('button');
  okBtn.textContent = confirmLabel;
  okBtn.disabled = true;
  okBtn.style.cssText = 'padding:10px 18px;border-radius:20px;border:0;background:#0d7377;color:#fff;font-size:14px;cursor:pointer;';
  foot.append(addr, cancelBtn, okBtn);
  card.append(head, mapWrap, foot);
  overlay.appendChild(card);
  document.body.appendChild(overlay);
  ['keydown', 'keyup', 'keypress', 'input', 'paste'].forEach((t) => overlay.addEventListener(t, (e) => e.stopPropagation()));

  let done = false;
  let last = null;
  const onKey = (e) => { if (e.key === 'Escape') finish(onCancel); };
  const finish = (fn) => {
    if (done) return;
    done = true;
    window.removeEventListener('keydown', onKey, true);
    window.__pwMapsFail = null;
    overlay.remove();
    fn();
  };
  window.addEventListener('keydown', onKey, true);
  cancelBtn.onclick = () => finish(onCancel);
  okBtn.onclick = () => {
    if (!last) return;
    const p = last;
    finish(() => onPicked(p.lat, p.lng, p.address, p.city, p.state, p.zip));
  };
  handle.close = () => finish(() => {});
  window.__pwMapsFail = () => finish(onError);

  const load = () => new Promise((resolve, reject) => {
    if (window.google && window.google.maps && window.google.maps.Map) { resolve(); return; }
    const waiting = window.__pwMapsWaiting || (window.__pwMapsWaiting = []);
    waiting.push([resolve, reject]);
    if (document.getElementById('pw-google-maps')) return;
    const settle = (ok) => {
      const all = window.__pwMapsWaiting || [];
      window.__pwMapsWaiting = [];
      all.forEach((w) => (ok ? w[0]() : w[1](new Error('maps'))));
    };
    window.__pwMapsReady = () => settle(true);
    window.gm_authFailure = () => { if (window.__pwMapsFail) window.__pwMapsFail(); };
    const s = document.createElement('script');
    s.id = 'pw-google-maps';
    s.async = true;
    s.src = 'https://maps.googleapis.com/maps/api/js?key=' + encodeURIComponent(apiKey) + '&libraries=places&region=IN&loading=async&callback=__pwMapsReady';
    s.onerror = () => { s.remove(); settle(false); };
    document.head.appendChild(s);
  });

  const start = (placesLib) => {
    const gm = window.google.maps;
    const places = placesLib || gm.places || null;
    const map = new gm.Map(mapEl, {
      center: { lat: lat, lng: lng },
      zoom: zoom,
      mapTypeControl: false,
      streetViewControl: false,
      fullscreenControl: false,
      clickableIcons: false,
      gestureHandling: 'greedy'
    });
    map.addListener('click', (e) => { if (e.latLng) map.panTo(e.latLng); });
    let geocoder = null;
    try { geocoder = new gm.Geocoder(); } catch (e) { geocoder = null; }
    let seq = 0;
    const describe = () => {
      const c = map.getCenter();
      if (!c) return;
      const mine = ++seq;
      const point = { lat: c.lat(), lng: c.lng(), address: '', city: '', state: '', zip: '' };
      last = point;
      okBtn.disabled = false;
      addr.textContent = point.lat.toFixed(6) + ', ' + point.lng.toFixed(6);
      if (!geocoder) return;
      geocoder.geocode({ location: c }).then((res) => {
        if (mine !== seq || !res || !res.results || !res.results.length) return;
        const r = res.results[0];
        const part = (type) => {
          const found = (r.address_components || []).find((x) => x.types.indexOf(type) >= 0);
          return found ? found.long_name : '';
        };
        point.address = r.formatted_address || '';
        point.city = part('locality') || part('sublocality') || part('administrative_area_level_3') || part('administrative_area_level_2');
        point.state = part('administrative_area_level_1');
        point.zip = part('postal_code');
        if (point.address) addr.textContent = point.address;
      }).catch(() => {});
    };
    map.addListener('idle', describe);
    const moveTo = (loc) => {
      map.setCenter(loc);
      map.setZoom(18);
    };

    const geocodeGo = (req) => {
      if (!geocoder) return;
      geocoder.geocode(req).then((res) => {
        if (res && res.results && res.results.length) moveTo(res.results[0].geometry.location);
      }).catch(() => {});
    };
    let token = null;
    const viaNew = (q) => {
      if (!token) token = new places.AutocompleteSessionToken();
      return places.AutocompleteSuggestion.fetchAutocompleteSuggestions({
        input: q,
        includedRegionCodes: ['in'],
        sessionToken: token,
        origin: map.getCenter()
      }).then((r) => r.suggestions.filter((s) => s.placePrediction).map((s) => {
        const pred = s.placePrediction;
        const item = {
          text: pred.text.toString(),
          go: () => {
            const place = pred.toPlace();
            token = null;
            place.fetchFields({ fields: ['location'] }).then(() => {
              if (place.location) moveTo(place.location);
            }).catch(() => geocodeGo({ placeId: pred.placeId }));
          }
        };
        return item;
      }));
    };
    let legacy = null;
    const viaLegacy = (q) => new Promise((resolve, reject) => {
      if (!legacy) legacy = new gm.places.AutocompleteService();
      legacy.getPlacePredictions({ input: q, componentRestrictions: { country: 'in' } }, (preds, status) => {
        if (status === 'ZERO_RESULTS') { resolve([]); return; }
        if (status !== 'OK' || !preds) { reject(new Error(status)); return; }
        resolve(preds.map((pr) => {
          const item = { text: pr.description, go: () => geocodeGo({ placeId: pr.place_id }) };
          return item;
        }));
      });
    });
    const viaGeocode = (q) => {
      if (!geocoder) return Promise.resolve([]);
      return geocoder.geocode({ address: q, componentRestrictions: { country: 'IN' } }).then((res) => res.results.slice(0, 6).map((r) => {
        const item = { text: r.formatted_address, go: () => moveTo(r.geometry.location) };
        return item;
      }));
    };
    const viaPhoton = (q) => {
      const c = map.getCenter();
      const url = photonUrl + '?limit=6&q=' + encodeURIComponent(q) + (c ? '&lat=' + c.lat() + '&lon=' + c.lng() : '');
      return fetch(url).then((r) => r.json()).then((j) => (j.features || [])
        .filter((f) => f.geometry && String((f.properties || {}).countrycode || '').toUpperCase() === 'IN')
        .map((f) => {
          const pr = f.properties;
          const parts = [pr.name, pr.street, pr.locality, pr.district, pr.city, pr.state, pr.postcode].filter((x) => x);
          const loc = { lat: f.geometry.coordinates[1], lng: f.geometry.coordinates[0] };
          const item = { text: parts.filter((x, i) => parts.indexOf(x) === i).join(', '), go: () => moveTo(loc) };
          return item;
        }));
    };
    const tiers = [];
    if (places && places.AutocompleteSuggestion) tiers.push(viaNew);
    if (gm.places && gm.places.AutocompleteService) tiers.push(viaLegacy);
    tiers.push(viaGeocode);
    tiers.push(viaPhoton);
    const search = (q) => tiers[0](q).catch(() => {
      if (tiers.length > 1) { tiers.shift(); return search(q); }
      return [];
    });
    const showList = (items) => {
      list.innerHTML = '';
      list.style.display = items.length ? 'block' : 'none';
      items.forEach((it) => {
        const row = document.createElement('div');
        row.textContent = it.text;
        row.style.cssText = 'padding:10px 14px;cursor:pointer;font-size:14px;border-bottom:1px solid #f0f0f0;';
        row.onmouseenter = () => { row.style.background = '#f1f3f4'; };
        row.onmouseleave = () => { row.style.background = ''; };
        row.onclick = () => {
          list.style.display = 'none';
          input.value = it.text;
          it.go();
        };
        list.appendChild(row);
      });
    };
    let timer = 0;
    let qseq = 0;
    input.addEventListener('input', () => {
      clearTimeout(timer);
      const q = input.value.trim();
      if (q.length < 3) { showList([]); return; }
      timer = setTimeout(() => {
        const mine = ++qseq;
        search(q).then((items) => { if (mine === qseq && !done) showList(items); });
      }, 300);
    });
    input.addEventListener('keydown', (e) => {
      if (e.key !== 'Enter') return;
      const first = list.firstChild;
      if (first) first.click();
    });
    input.focus();
  };

  load().then(() => {
    const gm = window.google.maps;
    const lib = (name) => (gm.importLibrary ? gm.importLibrary(name).catch(() => null) : Promise.resolve(null));
    return Promise.all([lib('maps'), lib('places'), lib('geocoding')]);
  }).then((libs) => {
    if (!done) start(libs[1]);
  }).catch(() => finish(onError));
  return handle;
})()""",
)
