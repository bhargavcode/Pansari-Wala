#!/usr/bin/env python3
"""Translate product names (master catalog + every shop's POS products) into the app languages.

Offline and deterministic, no translation service:
  1. PHRASES / WORDS   hand-written translations of grocery words in hi, kn, pa, mr, bn.
  2. DEVANAGARI        hand-written Devanagari spellings of brands and loanwords
                       ("Haldiram's" -> हल्दीराम्स), converted to Kannada / Gurmukhi / Bengali script.
  3. Phonetic fallback for any remaining word (English spelling -> Devanagari -> other scripts).
Pack sizes and numbers ("200g", "1L") are kept as written.

Usage (from repo root):
  python3 scripts/translate_products.py preview --limit 40          # cached catalog, no login
  python3 scripts/translate_products.py preview --name "Haldiram's Moong dal 200g"
  python3 scripts/translate_products.py apply                       # missing translations only
  python3 scripts/translate_products.py apply --overwrite --skip-shops
Needs the server with POST /admin/master/products/translations and /admin/shops/{id}/products/translations.
Admin password: ADMIN_PASSWORD env var or scripts/.catalog-cache/admin_password (same as seed_master_catalog.py).
"""
import argparse
import json
import os
import re
import sys
import urllib.parse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

LANGS = ("hi", "kn", "pa", "mr", "bn")
CHUNK = 200
CACHE_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".catalog-cache", "catalog_v2.json")

# Multi-word names first; "english=hi|kn|pa|mr|bn".
PHRASES = """
butter milk=छाछ|ಮಜ್ಜಿಗೆ|ਲੱਸੀ|ताक|ঘোল
buttermilk=छाछ|ಮಜ್ಜಿಗೆ|ਲੱਸੀ|ताक|ঘোল
ice cream=आइसक्रीम|ಐಸ್ ಕ್ರೀಮ್|ਆਈਸ ਕਰੀਮ|आईस्क्रीम|আইসক্রিম
hair oil=बालों का तेल|ಕೂದಲಿನ ಎಣ್ಣೆ|ਵਾਲਾਂ ਦਾ ਤੇਲ|केसांचे तेल|চুলের তেল
coconut oil=नारियल तेल|ತೆಂಗಿನ ಎಣ್ಣೆ|ਨਾਰੀਅਲ ਤੇਲ|खोबरेल तेल|নারকেল তেল
mustard oil=सरसों तेल|ಸಾಸಿವೆ ಎಣ್ಣೆ|ਸਰ੍ਹੋਂ ਦਾ ਤੇਲ|मोहरी तेल|সরিষার তেল
gulab jamun=गुलाब जामुन|ಗುಲಾಬ್ ಜಾಮೂನ್|ਗੁਲਾਬ ਜਾਮੁਨ|गुलाबजाम|গোলাপজাম
soan papdi=सोन पापड़ी|ಸೋನ್ ಪಾಪ್ಡಿ|ਸੋਨ ਪਾਪੜੀ|सोनपापडी|সোন পাপড়ি
peanut butter=पीनट बटर|ಪೀನಟ್ ಬಟರ್|ਪੀਨਟ ਬਟਰ|पीनट बटर|পিনাট বাটার
green tea=ग्रीन टी|ಗ್ರೀನ್ ಟೀ|ਗ੍ਰੀਨ ਟੀ|ग्रीन टी|গ্রিন টি
ice tea=आइस टी|ಐಸ್ ಟೀ|ਆਈਸ ਟੀ|आइस टी|আইস টি
iced tea=आइस्ड टी|ಐಸ್ಡ್ ಟೀ|ਆਈਸਡ ਟੀ|आइस्ड टी|আইসড টি
aloe vera=एलोवेरा|ಅಲೋವೆರಾ|ਐਲੋਵੇਰਾ|कोरफड|অ্যালোভেরা
corn flakes=कॉर्नफ्लेक्स|ಕಾರ್ನ್‌ಫ್ಲೇಕ್ಸ್|ਕੌਰਨਫਲੇਕਸ|कॉर्नफ्लेक्स|কর্নফ্লেক্স
face wash=फेस वॉश|ಫೇಸ್ ವಾಶ್|ਫੇਸ ਵਾਸ਼|फेस वॉश|ফেস ওয়াশ
hand wash=हैंड वॉश|ಹ್ಯಾಂಡ್ ವಾಶ್|ਹੈਂਡ ਵਾਸ਼|हँड वॉश|হ্যান্ড ওয়াশ
dish wash=डिश वॉश|ಡಿಶ್ ವಾಶ್|ਡਿਸ਼ ਵਾਸ਼|डिश वॉश|ডিশ ওয়াশ
garam masala=गरम मसाला|ಗರಂ ಮಸಾಲಾ|ਗਰਮ ਮਸਾਲਾ|गरम मसाला|গরম মশলা
red chilli=लाल मिर्च|ಕೆಂಪು ಮೆಣಸಿನಕಾಯಿ|ਲਾਲ ਮਿਰਚ|लाल मिरची|লাল লঙ্কা
green chilli=हरी मिर्च|ಹಸಿರು ಮೆಣಸಿನಕಾಯಿ|ਹਰੀ ਮਿਰਚ|हिरवी मिरची|কাঁচা লঙ্কা
black pepper=काली मिर्च|ಕರಿಮೆಣಸು|ਕਾਲੀ ਮਿਰਚ|काळी मिरी|গোলমরিচ
dark chocolate=डार्क चॉकलेट|ಡಾರ್ಕ್ ಚಾಕಲೇಟ್|ਡਾਰਕ ਚਾਕਲੇਟ|डार्क चॉकलेट|ডার্ক চকলেট
milk powder=मिल्क पाउडर|ಹಾಲಿನ ಪುಡಿ|ਦੁੱਧ ਪਾਊਡਰ|दूध पावडर|গুঁড়ো দুধ
chilli powder=मिर्च पाउडर|ಖಾರದ ಪುಡಿ|ਮਿਰਚ ਪਾਊਡਰ|तिखट|লঙ্কা গুঁড়ো
turmeric powder=हल्दी पाउडर|ಅರಿಶಿನ ಪುಡಿ|ਹਲਦੀ ਪਾਊਡਰ|हळद पावडर|হলুদ গুঁড়ো
coriander powder=धनिया पाउडर|ಕೊತ್ತಂಬರಿ ಪುಡಿ|ਧਨੀਆ ਪਾਊਡਰ|धणे पावडर|ধনে গুঁড়ো
cumin powder=जीरा पाउडर|ಜೀರಿಗೆ ಪುಡಿ|ਜੀਰਾ ਪਾਊਡਰ|जिरे पावडर|জিরে গুঁড়ো
toor dal=तूर दाल|ತೊಗರಿ ಬೇಳೆ|ਅਰਹਰ ਦਾਲ|तूर डाळ|অড়হর ডাল
chana dal=चना दाल|ಕಡಲೆ ಬೇಳೆ|ਛੋਲਿਆਂ ਦੀ ਦਾਲ|हरभरा डाळ|ছোলার ডাল
moong dal=मूंग दाल|ಹೆಸರು ಬೇಳೆ|ਮੂੰਗੀ ਦੀ ਦਾਲ|मूग डाळ|মুগ ডাল
urad dal=उड़द दाल|ಉದ್ದಿನ ಬೇಳೆ|ਮਾਂਹ ਦੀ ਦਾਲ|उडीद डाळ|কলাইয়ের ডাল
masoor dal=मसूर दाल|ಮಸೂರ್ ಬೇಳೆ|ਮਸਰ ਦੀ ਦਾਲ|मसूर डाळ|মসুর ডাল
basmati rice=बासमती चावल|ಬಾಸ್ಮತಿ ಅಕ್ಕಿ|ਬਾਸਮਤੀ ਚੌਲ|बासमती तांदूळ|বাসমতী চাল
wheat flour=गेहूं का आटा|ಗೋಧಿ ಹಿಟ್ಟು|ਕਣਕ ਦਾ ਆਟਾ|गव्हाचे पीठ|গমের আটা
gram flour=बेसन|ಕಡಲೆ ಹಿಟ್ಟು|ਵੇਸਣ|बेसन|বেসন
rice flour=चावल का आटा|ಅಕ್ಕಿ ಹಿಟ್ಟು|ਚੌਲਾਂ ਦਾ ਆਟਾ|तांदळाचे पीठ|চালের গুঁড়ো
sunflower oil=सूरजमुखी तेल|ಸೂರ್ಯಕಾಂತಿ ಎಣ್ಣೆ|ਸੂਰਜਮੁਖੀ ਤੇਲ|सूर्यफूल तेल|সূর্যমুখী তেল
groundnut oil=मूंगफली तेल|ಕಡಲೆಕಾಯಿ ಎಣ್ಣೆ|ਮੂੰਗਫਲੀ ਤੇਲ|शेंगदाणा तेल|চিনাবাদাম তেল
olive oil=जैतून का तेल|ಆಲಿವ್ ಎಣ್ಣೆ|ਜ਼ੈਤੂਨ ਦਾ ਤੇਲ|ऑलिव्ह तेल|জলপাই তেল
sesame oil=तिल का तेल|ಎಳ್ಳೆಣ್ಣೆ|ਤਿਲ ਦਾ ਤੇਲ|तिळाचे तेल|তিলের তেল
parle-g=पारले-जी|ಪಾರ್ಲೆ-ಜಿ|ਪਾਰਲੇ-ਜੀ|पारले-जी|পার্লে-জি
parle g=पारले-जी|ಪಾರ್ಲೆ-ಜಿ|ਪਾਰਲੇ-ਜੀ|पारले-जी|পার্লে-জি
coca cola=कोका-कोला|ಕೋಕಾ-ಕೋಲಾ|ਕੋਕਾ-ਕੋਲਾ|कोका-कोला|কোকা-কোলা
coca-cola=कोका-कोला|ಕೋಕಾ-ಕೋಲಾ|ਕੋਕਾ-ਕੋਲਾ|कोका-कोला|কোকা-কোলা
glucon-d=ग्लूकॉन-डी|ಗ್ಲೂಕಾನ್-ಡಿ|ਗਲੂਕੌਨ-ਡੀ|ग्लुकॉन-डी|গ্লুকন-ডি
glucon d=ग्लूकॉन-डी|ಗ್ಲೂಕಾನ್-ಡಿ|ਗਲੂਕੌਨ-ਡੀ|ग्लुकॉन-डी|গ্লুকন-ডি
old spice=ओल्ड स्पाइस|ಓಲ್ಡ್ ಸ್ಪೈಸ್|ਓਲਡ ਸਪਾਈਸ|ओल्ड स्पाइस|ওল্ড স্পাইস
red bull=रेड बुल|ರೆಡ್ ಬುಲ್|ਰੈੱਡ ਬੁੱਲ|रेड बुल|রেড বুল
"""

# Single words; "english=hi|kn|pa|mr|bn".
WORDS = """
milk=दूध|ಹಾಲು|ਦੁੱਧ|दूध|দুধ
powder=पाउडर|ಪುಡಿ|ਪਾਊਡਰ|पावडर|গুঁড়ো
oil=तेल|ಎಣ್ಣೆ|ਤੇਲ|तेल|তেল
tea=चाय|ಚಹಾ|ਚਾਹ|चहा|চা
chai=चाय|ಚಹಾ|ਚਾਹ|चहा|চা
butter=मक्खन|ಬೆಣ್ಣೆ|ਮੱਖਣ|लोणी|মাখন
rice=चावल|ಅಕ್ಕಿ|ਚੌਲ|तांदूळ|চাল
peanut=मूंगफली|ಕಡಲೆಕಾಯಿ|ਮੂੰਗਫਲੀ|शेंगदाणे|চিনাবাদাম
peanuts=मूंगफली|ಕಡಲೆಕಾಯಿ|ਮੂੰਗਫਲੀ|शेंगदाणे|চিনাবাদাম
groundnut=मूंगफली|ಕಡಲೆಕಾಯಿ|ਮੂੰਗਫਲੀ|शेंगदाणे|চিনাবাদাম
fruit=फल|ಹಣ್ಣು|ਫਲ|फळ|ফল
fruits=फल|ಹಣ್ಣುಗಳು|ਫਲ|फळे|ফল
dal=दाल|ಬೇಳೆ|ਦਾਲ|डाळ|ডাল
daal=दाल|ಬೇಳೆ|ਦਾਲ|डाळ|ডাল
flavour=स्वाद|ಸ್ವಾದ|ਸੁਆਦ|स्वाद|স্বাদ
flavor=स्वाद|ಸ್ವಾದ|ਸੁਆਦ|स्वाद|স্বাদ
flv=स्वाद|ಸ್ವಾದ|ਸੁਆਦ|स्वाद|স্বাদ
mango=आम|ಮಾವು|ਅੰਬ|आंबा|আম
pickle=अचार|ಉಪ್ಪಿನಕಾಯಿ|ਅਚਾਰ|लोणचे|আচার
achar=अचार|ಉಪ್ಪಿನಕಾಯಿ|ਅਚਾਰ|लोणचे|আচার
chilli=मिर्च|ಮೆಣಸಿನಕಾಯಿ|ਮਿਰਚ|मिरची|লঙ্কা
chili=मिर्च|ಮೆಣಸಿನಕಾಯಿ|ਮਿਰਚ|मिरची|লঙ্কা
chilly=मिर्च|ಮೆಣಸಿನಕಾಯಿ|ਮਿਰਚ|मिरची|লঙ্কা
mirch=मिर्च|ಮೆಣಸು|ਮਿਰਚ|मिरची|লঙ্কা
mirchi=मिर्च|ಮೆಣಸು|ਮਿਰਚ|मिरची|লঙ্কা
green=हरा|ಹಸಿರು|ਹਰਾ|हिरवा|সবুজ
ghee=घी|ತುಪ್ಪ|ਘਿਓ|तूप|ঘি
organic=जैविक|ಸಾವಯವ|ਜੈਵਿਕ|सेंद्रिय|জৈব
coconut=नारियल|ತೆಂಗಿನಕಾಯಿ|ਨਾਰੀਅਲ|नारळ|নারকেল
whole=साबुत|ಇಡೀ|ਸਾਬਤ|अख्खा|গোটা
chana=चना|ಕಡಲೆ|ਛੋਲੇ|हरभरा|ছোলা
channa=चना|ಕಡಲೆ|ਛੋਲੇ|हरभरा|ছোলা
juice=जूस|ಜ್ಯೂಸ್|ਜੂਸ|ज्यूस|জুস
bread=ब्रेड|ಬ್ರೆಡ್|ਬਰੈੱਡ|ब्रेड|পাউরুটি
basmati=बासमती|ಬಾಸ್ಮತಿ|ਬਾਸਮਤੀ|बासमती|বাসমতী
roasted=भुना|ಹುರಿದ|ਭੁੰਨਿਆ|भाजलेले|ভাজা
tomato=टमाटर|ಟೊಮೆಟೊ|ਟਮਾਟਰ|टोमॅटो|টমেটো
paneer=पनीर|ಪನೀರ್|ਪਨੀਰ|पनीर|পনির
honey=शहद|ಜೇನುತುಪ್ಪ|ਸ਼ਹਿਦ|मध|মধু
drink=पेय|ಪಾನೀಯ|ਪੀਣ ਵਾਲਾ|पेय|পানীয়
chicken=चिकन|ಚಿಕನ್|ਚਿਕਨ|चिकन|চিকেন
orange=संतरा|ಕಿತ್ತಳೆ|ਸੰਤਰਾ|संत्रा|কমলা
natural=प्राकृतिक|ನೈಸರ್ಗಿಕ|ਕੁਦਰਤੀ|नैसर्गिक|প্রাকৃতিক
lemon=नींबू|ನಿಂಬೆ|ਨਿੰਬੂ|लिंबू|লেবু
lime=नींबू|ನಿಂಬೆ|ਨਿੰਬੂ|लिंबू|লেবু
nimbu=नींबू|ನಿಂಬೆ|ਨਿੰਬੂ|लिंबू|লেবু
biscuit=बिस्किट|ಬಿಸ್ಕತ್ತು|ਬਿਸਕੁਟ|बिस्किट|বিস্কুট
biscuits=बिस्किट|ಬಿಸ್ಕತ್ತು|ਬਿਸਕੁਟ|बिस्किटे|বিস্কুট
pure=शुद्ध|ಶುದ್ಧ|ਸ਼ੁੱਧ|शुद्ध|খাঁটি
almond=बादाम|ಬಾದಾಮಿ|ਬਦਾਮ|बदाम|কাঠবাদাম
almonds=बादाम|ಬಾದಾಮಿ|ਬਦਾਮ|बदाम|কাঠবাদাম
badam=बादाम|ಬಾದಾಮಿ|ਬਦਾਮ|बदाम|কাঠবাদাম
with=के साथ|ಜೊತೆ|ਨਾਲ|सह|সহ
and=और|ಮತ್ತು|ਅਤੇ|आणि|ও
&=और|ಮತ್ತು|ਅਤੇ|आणि|ও
fresh=ताज़ा|ತಾಜಾ|ਤਾਜ਼ਾ|ताजे|টাটকা
bhujia=भुजिया|ಭುಜಿಯಾ|ਭੁਜੀਆ|भुजिया|ভুজিয়া
atta=आटा|ಗೋಧಿ ಹಿಟ್ಟು|ਆਟਾ|आटा|আটা
aata=आटा|ಗೋಧಿ ಹಿಟ್ಟು|ਆਟਾ|आटा|আটা
mixed=मिश्रित|ಮಿಶ್ರ|ਮਿਸ਼ਰਤ|मिश्र|মিশ্র
coffee=कॉफ़ी|ಕಾಫಿ|ਕੌਫੀ|कॉफी|কফি
garlic=लहसुन|ಬೆಳ್ಳುಳ್ಳಿ|ਲਸਣ|लसूण|রসুন
red=लाल|ಕೆಂಪು|ਲਾਲ|लाल|লাল
black=काला|ಕಪ್ಪು|ਕਾਲਾ|काळा|কালো
mixture=मिक्सचर|ಮಿಕ್ಸ್ಚರ್|ਮਿਕਸਚਰ|मिक्स्चर|মিক্সচার
aloo=आलू|ಆಲೂಗಡ್ಡೆ|ਆਲੂ|बटाटा|আলু
alu=आलू|ಆಲೂಗಡ್ಡೆ|ਆਲੂ|बटाटा|আলু
potato=आलू|ಆಲೂಗಡ್ಡೆ|ਆਲੂ|बटाटा|আলু
salt=नमक|ಉಪ್ಪು|ਲੂਣ|मीठ|লবণ
namak=नमक|ಉಪ್ಪು|ਲੂਣ|मीठ|লবণ
soap=साबुन|ಸಾಬೂನು|ਸਾਬਣ|साबण|সাবান
sugar=चीनी|ಸಕ್ಕರೆ|ਖੰਡ|साखर|চিনি
chutney=चटनी|ಚಟ್ನಿ|ਚਟਨੀ|चटणी|চাটনি
moong=मूंग|ಹೆಸರು|ਮੂੰਗੀ|मूग|মুগ
water=पानी|ನೀರು|ਪਾਣੀ|पाणी|জল
cashew=काजू|ಗೋಡಂಬಿ|ਕਾਜੂ|काजू|কাজু
cashews=काजू|ಗೋಡಂಬಿ|ਕਾਜੂ|काजू|কাজু
kaju=काजू|ಗೋಡಂಬಿ|ਕਾਜੂ|काजू|কাজু
dates=खजूर|ಖರ್ಜೂರ|ਖਜੂਰ|खजूर|খেজুর
cow=गाय|ಹಸು|ਗਾਂ|गाय|গরু
sweet=मीठा|ಸಿಹಿ|ਮਿੱਠਾ|गोड|মিষ্টি
meetha=मीठा|ಸಿಹಿ|ਮਿੱਠਾ|गोड|মিষ্টি
sweets=मिठाई|ಸಿಹಿತಿಂಡಿ|ਮਿਠਾਈ|मिठाई|মিষ্টি
wheat=गेहूं|ಗೋಧಿ|ਕਣਕ|गहू|গম
flour=आटा|ಹಿಟ್ಟು|ਆਟਾ|पीठ|আটা
hair=बाल|ಕೂದಲು|ਵਾਲ|केस|চুল
white=सफ़ेद|ಬಿಳಿ|ਚਿੱਟਾ|पांढरा|সাদা
corn=मक्का|ಮೆಕ್ಕೆಜೋಳ|ਮੱਕੀ|मका|ভুট্টা
ginger=अदरक|ಶುಂಠಿ|ਅਦਰਕ|आले|আদা
seeds=बीज|ಬೀಜಗಳು|ਬੀਜ|बिया|বীজ
seed=बीज|ಬೀಜ|ਬੀਜ|बी|বীজ
millet=बाजरा|ಸಿರಿಧಾನ್ಯ|ਬਾਜਰਾ|बाजरी|বাজরা
onion=प्याज़|ಈರುಳ್ಳಿ|ਪਿਆਜ਼|कांदा|পেঁয়াজ
jeera=जीरा|ಜೀರಿಗೆ|ਜੀਰਾ|जिरे|জিরে
cumin=जीरा|ಜೀರಿಗೆ|ਜੀਰਾ|जिरे|জিরে
salted=नमकीन|ಉಪ್ಪಿನ|ਨਮਕੀਨ|खारवलेले|নোনতা
nuts=मेवे|ಬೀಜಗಳು|ਮੇਵੇ|सुकामेवा|বাদাম
nut=मेवा|ಬೀಜ|ਮੇਵਾ|सुकामेवा|বাদাম
mustard=सरसों|ಸಾಸಿವೆ|ਸਰ੍ਹੋਂ|मोहरी|সরিষা
pista=पिस्ता|ಪಿಸ್ತಾ|ਪਿਸਤਾ|पिस्ता|পেস্তা
pistachio=पिस्ता|ಪಿಸ್ತಾ|ਪਿਸਤਾ|पिस्ता|পেস্তা
ragi=रागी|ರಾಗಿ|ਰਾਗੀ|नाचणी|রাগি
papad=पापड़|ಹಪ್ಪಳ|ਪਾਪੜ|पापड|পাঁপড়
dry=सूखा|ಒಣ|ਸੁੱਕਾ|सुका|শুকনো
dried=सूखा|ಒಣಗಿದ|ਸੁੱਕਿਆ|सुकवलेले|শুকনো
paper=कागज़|ಕಾಗದ|ਕਾਗਜ਼|कागद|কাগজ
spicy=तीखा|ಖಾರ|ਮਸਾਲੇਦਾਰ|तिखट|ঝাল
raw=कच्चा|ಹಸಿ|ਕੱਚਾ|कच्चा|কাঁচা
skin=त्वचा|ಚರ್ಮ|ਚਮੜੀ|त्वचा|ত্বক
apple=सेब|ಸೇಬು|ਸੇਬ|सफरचंद|আপেল
methi=मेथी|ಮೆಂತ್ಯ|ਮੇਥੀ|मेथी|মেথি
fenugreek=मेथी|ಮೆಂತ್ಯ|ਮੇਥੀ|मेथी|মেথি
biryani=बिरयानी|ಬಿರಿಯಾನಿ|ਬਿਰਯਾਨੀ|बिर्याणी|বিরিয়ানি
sev=सेव|ಸೇವ್|ਸੇਵ|शेव|সেও
kesar=केसर|ಕೇಸರಿ|ਕੇਸਰ|केशर|জাফরান
saffron=केसर|ಕೇಸರಿ|ਕੇਸਰ|केशर|জাফরান
pepper=काली मिर्च|ಮೆಣಸು|ਕਾਲੀ ਮਿਰਚ|मिरी|গোলমরিচ
rose=गुलाब|ಗುಲಾಬಿ|ਗੁਲਾਬ|गुलाब|গোলাপ
gulab=गुलाब|ಗುಲಾಬ್|ਗੁਲਾਬ|गुलाब|গোলাপ
khatta=खट्टा|ಹುಳಿ|ਖੱਟਾ|आंबट|টক
sour=खट्टा|ಹುಳಿ|ਖੱਟਾ|आंबट|টক
poha=पोहा|ಅವಲಕ್ಕಿ|ਪੋਹਾ|पोहे|চিঁড়ে
guava=अमरूद|ಸೀಬೆಹಣ್ಣು|ਅਮਰੂਦ|पेरू|পেয়ারা
liquid=तरल|ದ್ರವ|ਤਰਲ|द्रव|তরল
turmeric=हल्दी|ಅರಿಶಿನ|ਹਲਦੀ|हळद|হলুদ
haldi=हल्दी|ಅರಿಶಿನ|ਹਲਦੀ|हळद|হলুদ
plain=सादा|ಸಾದಾ|ਸਾਦਾ|साधा|সাধারণ
banana=केला|ಬಾಳೆಹಣ್ಣು|ਕੇਲਾ|केळी|কলা
chikki=चिक्की|ಚಿಕ್ಕಿ|ਚਿੱਕੀ|चिक्की|চিক্কি
rava=सूजी|ರವೆ|ਸੂਜੀ|रवा|সুজি
rawa=सूजी|ರವೆ|ਸੂਜੀ|रवा|সুজি
suji=सूजी|ರವೆ|ਸੂਜੀ|रवा|সুজি
sooji=सूजी|ರವೆ|ਸੂਜੀ|रवा|সুজি
semolina=सूजी|ರವೆ|ਸੂਜੀ|रवा|সুজি
idli=इडली|ಇಡ್ಲಿ|ਇਡਲੀ|इडली|ইডলি
idly=इडली|ಇಡ್ಲಿ|ਇਡਲੀ|इडली|ইডলি
grain=अनाज|ಧಾನ್ಯ|ਅਨਾਜ|धान्य|শস্য
rajma=राजमा|ರಾಜ್ಮಾ|ਰਾਜਮਾਂ|राजमा|রাজমা
jaggery=गुड़|ಬೆಲ್ಲ|ਗੁੜ|गूळ|গুড়
gur=गुड़|ಬೆಲ್ಲ|ਗੁੜ|गूळ|গুড়
urad=उड़द|ಉದ್ದು|ਮਾਂਹ|उडीद|কলাই
dahi=दही|ಮೊಸರು|ਦਹੀਂ|दही|দই
curd=दही|ಮೊಸರು|ਦਹੀਂ|दही|দই
yogurt=दही|ಮೊಸರು|ਦਹੀਂ|दही|দই
yoghurt=दही|ಮೊಸರು|ਦਹੀਂ|दही|দই
dosa=डोसा|ದೋಸೆ|ਡੋਸਾ|डोसा|দোসা
raisins=किशमिश|ಒಣದ್ರಾಕ್ಷಿ|ਸੌਗੀ|मनुका|কিশমিশ
raisin=किशमिश|ಒಣದ್ರಾಕ್ಷಿ|ਸੌਗੀ|मनुका|কিশমিশ
sunflower=सूरजमुखी|ಸೂರ್ಯಕಾಂತಿ|ਸੂਰਜਮੁਖੀ|सूर्यफूल|সূর্যমুখী
makhana=मखाना|ಮಖಾನಾ|ਮਖਾਣੇ|मखाणा|মাখানা
sesame=तिल|ಎಳ್ಳು|ਤਿਲ|तीळ|তিল
til=तिल|ಎಳ್ಳು|ਤਿਲ|तीळ|তিল
samosa=समोसा|ಸಮೋಸಾ|ਸਮੋਸਾ|सामोसा|শিঙাড়া
elaichi=इलायची|ಏಲಕ್ಕಿ|ਇਲਾਇਚੀ|वेलची|এলাচ
cardamom=इलायची|ಏಲಕ್ಕಿ|ਇਲਾਇਚੀ|वेलची|এলাচ
boondi=बूंदी|ಬೂಂದಿ|ਬੂੰਦੀ|बुंदी|বোঁদে
amla=आंवला|ನೆಲ್ಲಿಕಾಯಿ|ਔਲਾ|आवळा|আমলকি
vegetable=सब्ज़ी|ತರಕಾರಿ|ਸਬਜ਼ੀ|भाजी|সবজি
vegetables=सब्ज़ियां|ತರಕಾರಿಗಳು|ਸਬਜ਼ੀਆਂ|भाज्या|সবজি
mint=पुदीना|ಪುದೀನ|ਪੁਦੀਨਾ|पुदिना|পুদিনা
pudina=पुदीना|ಪುದೀನ|ਪੁਦੀਨਾ|पुदिना|পুদিনা
besan=बेसन|ಕಡಲೆ ಹಿಟ್ಟು|ਵੇਸਣ|बेसन|বেসন
coriander=धनिया|ಕೊತ್ತಂಬರಿ|ਧਨੀਆ|धणे|ধনে
dhaniya=धनिया|ಕೊತ್ತಂಬರಿ|ਧਨੀਆ|धणे|ধনে
jamun=जामुन|ಜಾಮೂನ್|ਜਾਮਣ|जांभूळ|জাম
garam=गरम|ಗರಂ|ਗਰਮ|गरम|গরম
masala=मसाला|ಮಸಾಲಾ|ਮਸਾਲਾ|मसाला|মশলা
sambar=सांभर|ಸಾಂಬಾರ್|ਸਾਂਭਰ|सांबार|সাম্বার
murukku=मुरुक्कु|ಮುರುಕು|ਮੁਰੁੱਕੂ|मुरुक्कू|মুরুক্কু
tamarind=इमली|ಹುಣಸೆಹಣ್ಣು|ਇਮਲੀ|चिंच|তেঁতুল
imli=इमली|ಹುಣಸೆಹಣ್ಣು|ਇਮਲੀ|चिंच|তেঁতুল
masoor=मसूर|ಮಸೂರ್|ਮਸਰ|मसूर|মসুর
yellow=पीला|ಹಳದಿ|ਪੀਲਾ|पिवळा|হলুদ
tasty=स्वादिष्ट|ರುಚಿಕರ|ਸੁਆਦੀ|चविष्ट|সুস্বাদু
neem=नीम|ಬೇವು|ਨਿੰਮ|कडुलिंब|নিম
namkeen=नमकीन|ನಮ್ಕೀನ್|ਨਮਕੀਨ|नमकीन|নোনতা
hing=हींग|ಇಂಗು|ਹਿੰਗ|हिंग|হিং
asafoetida=हींग|ಇಂಗು|ਹਿੰਗ|हिंग|হিং
palak=पालक|ಪಾಲಕ್|ਪਾਲਕ|पालक|পালং
spinach=पालक|ಪಾಲಕ್|ਪਾਲਕ|पालक|পালং
spice=मसाला|ಮಸಾಲೆ|ਮਸਾਲਾ|मसाला|মশলা
spices=मसाले|ಮಸಾಲೆಗಳು|ਮਸਾਲੇ|मसाले|মশলা
vermicelli=सेवई|ಶಾವಿಗೆ|ਸੇਵੀਆਂ|शेवया|সেমাই
malai=मलाई|ಕೆನೆ|ਮਲਾਈ|साय|মালাই
chiwda=चिवड़ा|ಚಿವ್ಡಾ|ਚਿਵੜਾ|चिवडा|চিঁড়ে ভাজা
chivda=चिवड़ा|ಚಿವ್ಡಾ|ਚਿਵੜਾ|चिवडा|চিঁড়ে ভাজা
eggs=अंडे|ಮೊಟ್ಟೆಗಳು|ਅੰਡੇ|अंडी|ডিম
egg=अंडा|ಮೊಟ್ಟೆ|ਅੰਡਾ|अंडे|ডিম
walnut=अखरोट|ಅಕ್ರೋಟ್|ਅਖਰੋਟ|अक्रोड|আখরোট
walnuts=अखरोट|ಅಕ್ರೋಟ್|ਅਖਰੋਟ|अक्रोड|আখরোট
leaf=पत्ती|ಎಲೆ|ਪੱਤੀ|पान|পাতা
leaves=पत्ते|ಎಲೆಗಳು|ਪੱਤੇ|पाने|পাতা
fish=मछली|ಮೀನು|ਮੱਛੀ|मासे|মাছ
olive=जैतून|ಆಲಿವ್|ਜ਼ੈਤੂਨ|ऑलिव्ह|জলপাই
matar=मटर|ಬಟಾಣಿ|ਮਟਰ|वाटाणा|মটর
peas=मटर|ಬಟಾಣಿ|ਮਟਰ|वाटाणा|মটর
maida=मैदा|ಮೈದಾ|ਮੈਦਾ|मैदा|ময়দা
pineapple=अनानास|ಅನಾನಸ್|ਅਨਾਨਾਸ|अननस|আনারস
pomegranate=अनार|ದಾಳಿಂಬೆ|ਅਨਾਰ|डाळिंब|ডালিম
roti=रोटी|ರೊಟ್ಟಿ|ਰੋਟੀ|पोळी|রুটি
chapati=चपाती|ಚಪಾತಿ|ਚਪਾਤੀ|चपाती|চাপাতি
tulsi=तुलसी|ತುಳಸಿ|ਤੁਲਸੀ|तुळस|তুলসী
fennel=सौंफ|ಸೋಂಪು|ਸੌਂਫ|बडीशेप|মৌরি
saunf=सौंफ|ಸೋಂಪು|ਸੌਂਫ|बडीशेप|মৌরি
peda=पेड़ा|ಪೇಡಾ|ਪੇੜਾ|पेढा|প্যাঁড়া
sandal=चंदन|ಶ್ರೀಗಂಧ|ਚੰਦਨ|चंदन|চন্দন
jasmine=चमेली|ಮಲ್ಲಿಗೆ|ਚਮੇਲੀ|मोगरा|জুঁই
vinegar=सिरका|ವಿನೆಗರ್|ਸਿਰਕਾ|व्हिनेगर|ভিনেগার
pulses=दालें|ಬೇಳೆಕಾಳುಗಳು|ਦਾਲਾਂ|डाळी|ডাল
pulse=दाल|ಬೇಳೆಕಾಳು|ਦਾਲ|डाळ|ডাল
jowar=ज्वार|ಜೋಳ|ਜਵਾਰ|ज्वारी|জোয়ার
bajra=बाजरा|ಸಜ್ಜೆ|ਬਾਜਰਾ|बाजरी|বাজরা
kabuli=काबुली|ಕಾಬೂಲಿ|ਕਾਬੁਲੀ|काबुली|কাবুলি
puja=पूजा|ಪೂಜೆ|ਪੂਜਾ|पूजा|পূজা
pooja=पूजा|ಪೂಜೆ|ਪੂਜਾ|पूजा|পূজা
agarbatti=अगरबत्ती|ಊದುಬತ್ತಿ|ਅਗਰਬੱਤੀ|अगरबत्ती|ধূপকাঠি
incense=अगरबत्ती|ಊದುಬತ್ತಿ|ਅਗਰਬੱਤੀ|अगरबत्ती|ধূপকাঠি
camphor=कपूर|ಕರ್ಪೂರ|ਕਪੂਰ|कापूर|কর্পূর
cloves=लौंग|ಲವಂಗ|ਲੌਂਗ|लवंग|লবঙ্গ
clove=लौंग|ಲವಂಗ|ਲੌਂਗ|लवंग|লবঙ্গ
cinnamon=दालचीनी|ದಾಲ್ಚಿನ್ನಿ|ਦਾਲਚੀਨੀ|दालचिनी|দারচিনি
ajwain=अजवाइन|ಓಮ|ਅਜਵਾਇਣ|ओवा|জোয়ান
the=||||
buttermilk=छाछ|ಮಜ್ಜಿಗೆ|ਲੱਸੀ|ताक|ঘোল
doodh=दूध|ಹಾಲು|ਦੁੱਧ|दूध|দুধ
apricot=खुबानी|ಏಪ್ರಿಕಾಟ್|ਖੁਰਮਾਨੀ|जर्दाळू|এপ্রিকট
sabudana=साबूदाना|ಸಬ್ಬಕ್ಕಿ|ਸਾਬੂਦਾਣਾ|साबुदाणा|সাবুদানা
ladoo=लड्डू|ಲಾಡು|ਲੱਡੂ|लाडू|লাড্ডু
laddu=लड्डू|ಲಾಡು|ਲੱਡੂ|लाडू|লাড্ডু
dalia=दलिया|ದಲಿಯಾ|ਦਲੀਆ|दलिया|ডালিয়া
toor=तूर|ತೊಗರಿ|ਅਰਹਰ|तूर|অড়হর
tuar=तूर|ತೊಗರಿ|ਅਰਹਰ|तूर|অড়হর
mutter=मटर|ಬಟಾಣಿ|ਮਟਰ|वाटाणा|মটর
pea=मटर|ಬಟಾಣಿ|ਮਟਰ|वाटाणा|মটর
gingelly=तिल|ಎಳ್ಳು|ਤਿਲ|तीळ|তিল
pappadam=पापड़|ಹಪ್ಪಳ|ਪਾਪੜ|पापड|পাঁপড়
sambhar=सांभर|ಸಾಂಬಾರ್|ਸਾਂਭਰ|सांबार|সাম্বার
chhole=छोले|ಕಡಲೆ|ਛੋਲੇ|छोले|ছোলে
choley=छोले|ಕಡಲೆ|ਛੋਲੇ|छोले|ছোলে
chole=छोले|ಕಡಲೆ|ਛੋਲੇ|छोले|ছোলে
rock=सेंधा|ಕಲ್ಲು|ਸੇਂਧਾ|सैंधव|সৈন্ধব
fried=तला हुआ|ಕರಿದ|ਤਲਿਆ|तळलेले|ভাজা
stuffed=भरवां|ತುಂಬಿದ|ਭਰਵਾਂ|भरलेले|পুর ভরা
sliced=कटा हुआ|ಹೋಳು ಮಾಡಿದ|ਕੱਟਿਆ|चिरलेले|কাটা
small=छोटा|ಸಣ್ಣ|ਛੋਟਾ|लहान|ছোট
new=नया|ಹೊಸ|ਨਵਾਂ|नवीन|নতুন
of=||||
cut=कटा|ಕತ್ತರಿಸಿದ|ਕੱਟਿਆ|कापलेले|কাটা
"""

# Devanagari spellings of brands and loanwords; other scripts are derived.
DEVANAGARI = """
haldiram's=हल्दीराम्स
haldirams=हल्दीराम्स
haldiram=हल्दीराम
britannia=ब्रिटानिया
parle=पारले
good=गुड
noodles=नूडल्स
noodle=नूडल
cookies=कुकीज़
cookie=कुकी
chocolate=चॉकलेट
chocolates=चॉकलेट्स
amul=अमूल
cadbury=कैडबरी
life=लाइफ
protein=प्रोटीन
sauce=सॉस
classic=क्लासिक
premium=प्रीमियम
choco=चोको
instant=इंस्टेंट
wash=वॉश
ashoka=अशोका
sunfeast=सनफीस्ट
oats=ओट्स
dairy=डेयरी
dabur=डाबर
real=रियल
bar=बार
tata=टाटा
maggi=मैगी
cake=केक
cheese=चीज़
dark=डार्क
soya=सोया
himalaya=हिमालया
face=फेस
vanilla=वनीला
magic=मैजिक
patanjali=पतंजलि
unibic=यूनीबिक
horlicks=हॉर्लिक्स
de=डी
hot=हॉट
shampoo=शैम्पू
veg=वेज
gel=जेल
brown=ब्राउन
body=बॉडी
original=ओरिजिनल
curry=करी
paste=पेस्ट
rusk=रस्क
ice=आइस
pet=पेट
indian=इंडियन
aachi=आची
mtr=एमटीआर
bikano=बीकानो
paratha=पराठा
minute=मिनट
pasta=पास्ता
shake=शेक
lotion=लोशन
delight=डिलाइट
gl=जीएल
marie=मैरी
muesli=म्यूज़ली
everest=एवरेस्ट
crunchy=क्रंची
sandwich=सैंडविच
choice=चॉइस
super=सुपर
strawberry=स्ट्रॉबेरी
whey=व्हे
cola=कोला
flavoured=फ्लेवर्ड
flavored=फ्लेवर्ड
balaji=बालाजी
godrej=गोदरेज
kellogg's=केलॉग्स
kelloggs=केलॉग्स
wafers=वेफर्स
wafer=वेफर
day=डे
plus=प्लस
foods=फ़ूड्स
food=फ़ूड
fortune=फॉर्च्यून
wai=वाई
flakes=फ्लेक्स
mdh=एमडीएच
soup=सूप
ketchup=केचप
creamy=क्रीमी
jam=जैम
desi=देसी
eastern=ईस्टर्न
lay's=लेज़
lays=लेज़
bourbon=बॉर्बन
free=फ्री
royal=रॉयल
punjabi=पंजाबी
saffola=सफोला
refined=रिफाइंड
catch=कैच
mini=मिनी
nivea=निविया
treat=ट्रीट
tandoori=तंदूरी
beauty=ब्यूटी
bikaji=बीकाजी
cocoa=कोको
campa=कैम्पा
digestive=डाइजेस्टिव
nutri=न्यूट्री
boat=बोट
peri=पेरी
in=इन
oreo=ओरियो
aashirvaad=आशीर्वाद
bingo=बिंगो
mother=मदर
mother's=मदर्स
knorr=नॉर
rich=रिच
bakri=बकरी
hide=हाइड
spread=स्प्रेड
health=हेल्थ
care=केयर
herbal=हर्बल
wagh=वाघ
seek=सीक
malt=माल्ट
pond's=पॉन्ड्स
ponds=पॉन्ड्स
pond=पॉन्ड
bites=बाइट्स
bite=बाइट
glow=ग्लो
veeba=वीबा
all=ऑल
modern=मॉडर्न
flipkart=फ्लिपकार्ट
kissan=किसान
toned=टोंड
syrup=सिरप
hershey's=हर्शीज़
tropicana=ट्रॉपिकाना
light=लाइट
lite=लाइट
double=डबल
nandini=नंदिनी
candy=कैंडी
farm=फ़ार्म
farms=फ़ार्म्स
coca=कोका
india=इंडिया
parachute=पैराशूट
nescafe=नेस्कैफ़े
pizza=पिज़्ज़ा
daily=डेली
multigrain=मल्टीग्रेन
happy=हैप्पी
mantra=मंत्रा
pack=पैक
energy=एनर्जी
bond=बॉन्ड
up=अप
agro=एग्रो
multi=मल्टी
roll=रोल
extra=एक्स्ट्रा
kitchen=किचन
mayonnaise=मेयोनेज़
chunks=चंक्स
soan=सोन
papdi=पापड़ी
first=फर्स्ट
style=स्टाइल
dettol=डेटॉल
yoga=योगा
pro=प्रो
gits=गिट्स
hakka=हक्का
vitamin=विटामिन
premia=प्रीमिया
rs=रु
masti=मस्ती
yippee=यिप्पी
milky=मिल्की
chip=चिप
chips=चिप्स
special=स्पेशल
puri=पूरी
kwality=क्वालिटी
golden=गोल्डन
gold=गोल्ड
brooke=ब्रुक
spray=स्प्रे
crop=क्रॉप
vaseline=वैसलीन
colgate=कोलगेट
blue=ब्लू
power=पावर
fantasy=फैंटेसी
dr.=डॉ.
dr=डॉ
max=मैक्स
lipton=लिप्टन
lassi=लस्सी
creme=क्रीम
cream=क्रीम
truly=ट्रूली
time=टाइम
bru=ब्रू
recipe=रेसिपी
khakhra=खाखरा
tree=ट्री
vera=वेरा
aloe=एलो
alovera=एलोवेरा
makhani=मखनी
american=अमेरिकन
ching's=चिंग्स
cracker=क्रैकर
crackers=क्रैकर्स
snack=स्नैक
snacks=स्नैक्स
bombay=बॉम्बे
crunch=क्रंच
nissin=निसिन
nutrition=न्यूट्रिशन
tiger=टाइगर
lal=लाल
popcorn=पॉपकॉर्न
mr=मिस्टर
cbd=सीबीडी
grocery=ग्रॉसरी
patanwala=पाटनवाला
schezwan=शेज़वान
munch=मंच
secret=सीक्रेट
oetker=ओटकर
mist=मिस्ट
dove=डव
top=टॉप
sunrise=सनराइज़
serum=सीरम
kulfi=कुल्फी
nestle=नेस्ले
big=बिग
more=मोर
kurkure=कुरकुरे
high=हाई
truth=ट्रुथ
heritage=हेरिटेज
ramen=रामेन
taj=ताज
best=बेस्ट
madras=मद्रास
sunscreen=सनस्क्रीन
himalayan=हिमालयन
strong=स्ट्रॉन्ग
bhel=भेल
home=होम
cleaner=क्लीनर
lizol=लाइज़ोल
toothpaste=टूथपेस्ट
pepsi=पेप्सी
active=एक्टिव
brand=ब्रांड
handwash=हैंडवॉश
hand=हैंड
soft=सॉफ्ट
surf=सर्फ़
shahi=शाही
soda=सोडा
scrub=स्क्रब
fun=फन
star=स्टार
chat=चाट
chaat=चाट
long=लॉन्ग
riz=रिज़
bournvita=बॉर्नविटा
cold=कोल्ड
naan=नान
chatpata=चटपटा
monaco=मोनैको
to=टू
bakery=बेकरी
goodlife=गुडलाइफ
hazelnut=हेज़लनट
act=एक्ट
zero=ज़ीरो
excel=एक्सेल
cornflakes=कॉर्नफ्लेक्स
little=लिटिल
bright=ब्राइट
caramel=कैरेमल
pani=पानी
toastea=टोस्टी
mahal=महल
nice=नाइस
ghani=घानी
pav=पाव
storia=स्टोरिया
silk=सिल्क
sun=सन
air=एयर
for=फॉर
my=माय
butterscotch=बटरस्कॉच
healthy=हेल्दी
harvest=हार्वेस्ट
sampann=सम्पन्न
mario=मारियो
coke=कोक
tadka=तड़का
cup=कप
nature=नेचर
glucose=ग्लूकोज़
daawat=दावत
chyawanprash=च्यवनप्राश
chocos=चोकोस
lion=लायन
mask=मास्क
greek=ग्रीक
dev=देव
pintola=पिंटोला
weikfield=वीकफ़ील्ड
tpk=टीपीके
virgin=वर्जिन
tattva=तत्त्व
litchi=लीची
lychee=लीची
navratan=नवरतन
label=लेबल
karachi=कराची
unsweetened=अनस्वीटन्ड
luxury=लक्ज़री
aroma=अरोमा
kohinoor=कोहिनूर
muscleblaze=मसलब्लेज़
mysore=मैसूर
santoor=संतूर
mad=मैड
kitkat=किटकैट
sundrop=सनड्रॉप
cranberry=क्रैनबेरी
happilo=हैप्पीलो
milkshake=मिल्कशेक
bakewell=बेकवेल
biotique=बायोटिक
batter=बैटर
breakfast=ब्रेकफास्ट
true=ट्रू
elements=एलिमेंट्स
sticks=स्टिक्स
stick=स्टिक
milkymist=मिल्कीमिस्ट
jabsons=जैबसन्स
jack=जैक
galaxy=गैलेक्सी
farmley=फार्मली
zandu=झंडू
enriched=एनरिच्ड
dmart=डीमार्ट
dnv=डीएनवी
cleanser=क्लींज़र
vicks=विक्स
shower=शावर
frooti=फ्रूटी
spiced=स्पाइस्ड
regular=रेगुलर
chakki=चक्की
bikis=बिकीस
get=गेट
pulp=पल्प
nacho=नाचो
nachos=नाचोस
veggie=वेजी
garden=गार्डन
bikaneri=बीकानेरी
apis=एपिस
funfoods=फनफूड्स
kachi=कच्ची
pillsbury=पिल्सबरी
mamaearth=मामाअर्थ
on=ऑन
epigamia=एपिगामिया
meat=मीट
apollo=अपोलो
premix=प्रीमिक्स
wipes=वाइप्स
tac=टैक
kashmiri=कश्मीरी
smooth=स्मूथ
badshah=बादशाह
balm=बाम
gentle=जेंटल
herbs=हर्ब्स
snickers=स्निकर्स
cool=कूल
chupa=चुपा
chups=चुप्स
jelly=जेली
can=कैन
protinex=प्रोटीनेक्स
coco=कोको
twist=ट्विस्ट
ganesh=गणेश
angles=एंगल्स
me=मी
aarambh=आरंभ
plum=प्लम
tofu=टोफू
cornitos=कॉर्निटोस
crisps=क्रिस्प्स
jersey=जर्सी
one=वन
bun=बन
johnson's=जॉनसन्स
advanced=एडवांस्ड
freshner=फ्रेशनर
horse=हॉर्स
durga=दुर्गा
diet=डाइट
deodorant=डियोड्रेंट
bisleri=बिसलेरी
maaza=माज़ा
quaker=क्वेकर
del=डेल
monte=मोंटे
slice=स्लाइस
slices=स्लाइस
taste=टेस्ट
whitener=व्हाइटनर
moisture=मॉइस्चर
harpic=हार्पिक
rings=रिंग्स
swing=स्विंग
facial=फेशियल
ready=रेडी
gum=गम
dukes=ड्यूक्स
tang=टैंग
everyday=एवरीडे
valley=वैली
alpenliebe=अल्पेनलिबे
puff=पफ
lovely=लवली
kernels=कर्नेल्स
tetley=टेटली
pancake=पैनकेक
unpolished=अनपॉलिश्ड
khari=खारी
grb=जीआरबी
sona=सोना
family=फैमिली
nutty=नटी
lakme=लैक्मे
katli=कतली
chitra=चित्रा
master=मास्टर
lip=लिप
cetaphil=सीटाफिल
drinking=ड्रिंकिंग
clean=क्लीन
go=गो
macaroni=मैकरोनी
oven=ओवन
vita=विटा
facewash=फेसवॉश
clear=क्लियर
wall's=वॉल्स
tangy=टैंगी
no=नो
kool=कूल
joy=जॉय
sprite=स्प्राइट
redbull=रेडबुल
fanta=फैंटा
thums=थम्स
limca=लिम्का
mix=मिक्स
juicy=जूसी
chewing=च्यूइंग
orbit=ऑर्बिट
wrigley's=रिग्लीज़
sensodyne=सेंसोडाइन
livon=लिवॉन
anti=एंटी
frizz=फ्रिज़
repair=रिपेयर
protect=प्रोटेक्ट
moisturizer=मॉइस्चराइज़र
moisturiser=मॉइस्चराइज़र
cream's=क्रीम्स
toast=टोस्ट
salsa=सालसा
filter=फ़िल्टर
grand=ग्रैंड
continental=कॉन्टिनेंटल
italian=इटैलियन
jar=जार
fc=एफसी
spf=एसपीएफ
uht=यूएचटी
ors=ओआरएस
ctc=सीटीसी
xl=एक्सएल
jk=जेके
ii=II
full=फुल
frozen=फ्रोज़न
coated=कोटेड
podi=पोडी
khana=खाना
deep=डीप
fit=फिट
bhaji=भाजी
tikka=टिक्का
complan=कॉम्प्लान
belgian=बेल्जियन
yogabar=योगाबार
pediasure=पीडियाश्योर
shakti=शक्ति
nutraj=न्यूट्राज
diamond=डायमंड
chia=चिया
ayurvedic=आयुर्वेदिक
vada=वड़ा
dhokla=ढोकला
fab=फैब
plant=प्लांट
vadilal=वाडीलाल
malabar=मालाबार
split=स्प्लिट
rasam=रसम
california=कैलिफ़ोर्निया
puttu=पुट्टु
kathi=काठी
mountain=माउंटेन
dew=ड्यू
keya=केया
blend=ब्लेंड
pain=पेन
quinoa=क्विनोआ
cocktail=कॉकटेल
maggie=मैगी
vlcc=वीएलसीसी
manchurian=मंचूरियन
cereal=सीरियल
berry=बेरी
pie=पाई
gathiya=गाठिया
soulfull=सोलफुल
cavin's=कैविन्स
total=टोटल
maid=मेड
milano=मिलानो
burger=बर्गर
mcvities=मैकविटीज़
cremica=क्रेमिका
brownie=ब्राउनी
crown=क्राउन
fat=फैट
slim=स्लिम
mothers=मदर्स
nan=नान
farali=फराली
kit=किट
men=मेन
too=टू
bran=ब्रान
korean=कोरियन
ayurveda=आयुर्वेद
perfume=परफ्यूम
floor=फ्लोर
rozana=रोज़ाना
tikki=टिक्की
khaman=खमण
king=किंग
jaimin=जैमिन
ram=राम
bandhu=बंधु
britania=ब्रिटानिया
bhelpuri=भेलपुरी
safe=सेफ
filled=फिल्ड
made=मेड
boost=बूस्ट
relief=रिलीफ
chunky=चंकी
soy=सोया
fills=फिल्स
pasteurised=पाश्चराइज़्ड
pasteurized=पाश्चराइज़्ड
fizz=फ़िज़
alphonso=अल्फांसो
oatmeal=ओटमील
jimjam=जिमजैम
dodla=डोडला
park=पार्क
collection=कलेक्शन
earth=अर्थ
elite=एलीट
lotte=लोटे
manna=मन्ना
doritos=डोरिटोस
balls=बॉल्स
ball=बॉल
smoodh=स्मूध
nutrela=न्यूट्रेला
gobbles=गॉबल्स
naturals=नैचुरल्स
shine=शाइन
wet=वेट
sea=सी
moisturizing=मॉइस्चराइज़िंग
moisturising=मॉइस्चराइज़िंग
wild=वाइल्ड
rolled=रोल्ड
krack=क्रैक
yumm=यम
abbott=एबट
ensure=एनश्योर
tops=टॉप्स
akshayakalpa=अक्षयकल्प
drops=ड्रॉप्स
kerala=केरल
thick=थिक
vim=विम
conditioner=कंडीशनर
knight=नाइट
lavender=लैवेंडर
isabgol=ईसबगोल
freshener=फ्रेशनर
nylon=नायलॉन
sakthi=शक्ति
bell=बेल
gate=गेट
prabhuji=प्रभुजी
winkin=विंकिन
shrikhand=श्रीखंड
gatorade=गेटोरेड
gram=ग्राम
sunfest=सनफेस्ट
kiwi=कीवी
delite=डिलाइट
mayo=मेयो
jumbo=जंबो
fall=फ़ॉल
floral=फ्लोरल
disinfectant=डिसइन्फेक्टेंट
talcum=टैल्कम
dandruff=डैंड्रफ
gillette=जिलेट
low=लो
kinley=किनले
priya=प्रिया
hajmola=हाजमोला
easy=ईज़ी
pringles=प्रिंगल्स
appy=ऐप्पी
intense=इंटेंस
moms=मॉम्स
mom's=मॉम्स
nectar=नेक्टर
chocobakes=चोकोबेक्स
cheesy=चीज़ी
cider=साइडर
garnier=गार्नियर
man=मैन
refreshing=रिफ्रेशिंग
pears=पियर्स
kachori=कचौरी
moringa=मोरिंगा
medium=मीडियम
zahidi=ज़ाहिदी
bags=बैग्स
quick=क्विक
reliance=रिलायंस
blueberry=ब्लूबेरी
dhara=धारा
manchow=मनचाउ
select=सेलेक्ट
dryfruit=ड्राईफ्रूट
churna=चूर्ण
pulpy=पल्पी
hydra=हाइड्रा
ultra=अल्ट्रा
krishna=कृष्णा
gluco=ग्लूको
cherry=चेरी
standard=स्टैंडर्ड
bisk=बिस्क
pulao=पुलाव
crush=क्रश
chocochip=चोकोचिप
blast=ब्लास्ट
minis=मिनीज़
jalapeno=हालापेन्यो
society=सोसाइटी
pop=पॉप
granola=ग्रेनोला
winkies=विंकीज़
gourmet=गौर्मे
wingreens=विनग्रीन्स
danone=डेनोन
chef=शेफ
burst=बर्स्ट
ruchi=रुचि
expert=एक्सपर्ट
bagrry's=बैगरीज़
eat=ईट
oat=ओट
pass=पास
cone=कोन
hit=हिट
miracle=मिरेकल
sat=सत
so=सो
falooda=फालूदा
gluten=ग्लूटेन
porotta=परोटा
meal=मील
upma=उपमा
supreme=सुप्रीम
gurujee=गुरुजी
steel=स्टील
alpino=अल्पिनो
rasna=रसना
agni=अग्नि
aqua=एक्वा
grandma's=ग्रैंडमाज़
pedigree=पेडिग्री
maybelline=मेबेलीन
shave=शेव
extract=एक्सट्रैक्ट
glucon=ग्लूकॉन
pressery=प्रेसरी
zoff=ज़ॉफ़
detergent=डिटर्जेंट
sofit=सोफिट
snac=स्नैक
frut=फ्रूट
snactac=स्नैकटैक
anmol=अनमोल
ritebite=राइटबाइट
disano=डिसानो
independence=इंडिपेंडेंस
myfitness=माईफिटनेस
pink=पिंक
co=को
funfood=फनफ़ूड
heinz=हाइंज़
mewati=मेवाती
baby=बेबी
john=जॉन
tide=टाइड
vatika=वाटिका
granny's=ग्रैनीज़
gooday=गुडडे
"""

LETTER_NAMES = {
    "a": "ए", "b": "बी", "c": "सी", "d": "डी", "e": "ई", "f": "एफ", "g": "जी", "h": "एच", "i": "आई",
    "j": "जे", "k": "के", "l": "एल", "m": "एम", "n": "एन", "o": "ओ", "p": "पी", "q": "क्यू", "r": "आर",
    "s": "एस", "t": "टी", "u": "यू", "v": "वी", "w": "डब्ल्यू", "x": "एक्स", "y": "वाई", "z": "ज़ेड",
}


def parse_table(text):
    table = {}
    for line in text.strip().splitlines():
        key, _, value = line.partition("=")
        parts = value.split("|")
        table[key.strip().lower()] = dict(zip(LANGS, (p.strip() for p in parts)))
    return table


def parse_dev(text):
    return {k.strip().lower(): v.strip() for k, _, v in (l.partition("=") for l in text.strip().splitlines())}


PHRASE_TABLE = parse_table(PHRASES)
WORD_TABLE = parse_table(WORDS)
DEV_TABLE = parse_dev(DEVANAGARI)
MAX_PHRASE_WORDS = max(len(k.split()) for k in PHRASE_TABLE)

# ---------------------------------------------------------------- English -> Devanagari (fallback)

V_INDEP = {"a": "अ", "aa": "आ", "i": "इ", "ee": "ई", "u": "उ", "oo": "ऊ", "e": "ए", "ai": "ऐ", "o": "ओ", "au": "औ",
           "ou": "औ", "ea": "ई", "ie": "ई", "oa": "ओ", "ay": "ए", "ey": "ए", "ei": "ई", "ui": "उइ", "oi": "ऑय", "oy": "ऑय",
           "aw": "ऑ", "ew": "यू", "ow": "ओ"}
V_MATRA = {"a": "", "aa": "ा", "i": "ि", "ee": "ी", "u": "ु", "oo": "ू", "e": "े", "ai": "ै", "o": "ो", "au": "ौ",
           "ou": "ौ", "ea": "ी", "ie": "ी", "oa": "ो", "ay": "े", "ey": "े", "ei": "ी", "ui": "ुइ", "oi": "ॉय", "oy": "ॉय",
           "aw": "ॉ", "ew": "्यू", "ow": "ो"}
CONS = {"sh": "श", "ch": "च", "th": "थ", "ph": "फ", "kh": "ख", "gh": "घ", "bh": "भ", "dh": "ध", "jh": "झ", "ck": "क",
        "wh": "व", "b": "ब", "d": "ड", "f": "फ", "g": "ग", "h": "ह", "j": "ज", "k": "क", "l": "ल", "m": "म", "n": "न",
        "p": "प", "q": "क", "r": "र", "s": "स", "t": "ट", "v": "व", "w": "व", "y": "य", "z": "ज़"}
VOWEL_KEYS = sorted(V_MATRA, key=len, reverse=True)
CONS_KEYS = sorted(CONS, key=len, reverse=True)
VIRAMA = "्"


def _units(word):
    """Splits a lowercase Latin word into ('C', dev) / ('V', key) units."""
    out, i = [], 0
    while i < len(word):
        ch = word[i]
        if ch == "x":
            out += [("C", "क"), ("C", "स")]
            i += 1
            continue
        if ch == "c":
            nxt = word[i + 1:i + 2]
            if word[i:i + 2] in ("ch", "ck"):
                pass
            else:
                out.append(("C", "स" if nxt in ("e", "i", "y") else "क"))
                i += 1
                continue
        if ch == "y" and out and out[-1][0] == "C" and (i + 1 == len(word) or word[i + 1] not in "aeiou"):
            out.append(("V", "ee"))
            i += 1
            continue
        matched = False
        for key in VOWEL_KEYS:
            if key in ("aw", "ew", "ow") and word[i + 2:i + 3] in tuple("aeiouy") and i + 2 < len(word):
                continue
            if key == "ow" and i + 2 != len(word):
                continue
            if word.startswith(key, i):
                out.append(("V", key))
                i += len(key)
                matched = True
                break
        if matched:
            continue
        for key in CONS_KEYS:
            if word.startswith(key, i):
                dev = CONS[key]
                if out and out[-1] == ("C", dev) and len(key) == 1:
                    i += 1  # doubled letter
                else:
                    out.append(("C", dev))
                    i += len(key)
                matched = True
                break
        if not matched:
            i += 1
    return out


def to_devanagari(word):
    w = word.lower()
    if not w.isalpha():
        return word
    if len(w) > 3 and w.endswith("e") and not w.endswith(("ee", "ie", "ye")) and w[-2] not in "aeiou":
        stem, magic = w[:-1], True
    else:
        stem, magic = w, False
    units = _units(stem)
    if magic:
        for k in range(len(units) - 1, -1, -1):
            if units[k][0] == "V":
                units[k] = ("V", {"a": "e", "i": "ai_", "o": "o", "u": "oo"}.get(units[k][1], units[k][1]))
                break
    res = []
    for idx, (kind, val) in enumerate(units):
        nxt = units[idx + 1] if idx + 1 < len(units) else None
        if kind == "V":
            if val == "ai_":
                res.append("ाइ" if res and res[-1] not in V_INDEP.values() else "आइ")
                continue
            prev_c = idx > 0 and units[idx - 1][0] == "C"
            if val == "a" and nxt is None and prev_c:
                res.append("ा")
            elif val == "a" and nxt and nxt == ("C", "र") and prev_c:
                res.append("ा")
            elif prev_c:
                res.append(V_MATRA[val])
            else:
                res.append(V_INDEP[val])
            continue
        if nxt and nxt[0] == "C":
            if val in ("न", "म") and nxt[1] not in ("य", "र", "ह", "व"):
                res.append("ं")
            else:
                res.append(val + VIRAMA)
        else:
            res.append(val)
    return "".join(res)


# ---------------------------------------------------------------- Devanagari -> other scripts

NUKTA_BASE = {"क़": "क", "ख़": "ख", "ग़": "ग", "ज़": "ज", "ड़": "ड़", "ढ़": "ढ़", "फ़": "फ", "य़": "य"}
DEV_CONS = set(chr(c) for c in range(0x0915, 0x093A))


def _decompose(text):
    return "".join(NUKTA_BASE.get(ch, ch) for ch in text)


MATRAS = set(chr(c) for c in range(0x093E, 0x094D)) | {"ॢ", "ॣ"}
CODA = {"ं", "ँ", "ः", VIRAMA}


def delete_schwa(word):
    """Hindi drops the inherent vowel in VC(a)CV ("रेडबुल" = red-bul); Kannada needs that virama written."""
    chars = list(word)
    n = len(chars)
    out = []
    i = 0
    while i < n:
        ch = chars[i]
        if ch not in DEV_CONS:
            out.append(ch)
            i += 1
            continue
        prev = out[-1] if out else ""
        out.append(ch)
        j = i + 1
        if j < n and chars[j] == "़":
            out.append("़")
            j += 1
        has_schwa = j < n and chars[j] not in MATRAS and chars[j] not in CODA
        if has_schwa and prev:
            prev_open = prev in MATRAS or prev in DEV_CONS or prev == "़" or "\u0904" <= prev <= "\u0914"
            k = j
            while k + 1 < n and chars[k] in DEV_CONS and chars[k + 1] == VIRAMA:
                k += 2
            nxt_full = k + 1 < n and chars[k] in DEV_CONS and chars[k] != "य" and chars[k + 1] in MATRAS
            if prev_open and nxt_full:
                out.append(VIRAMA)
        i = j
    return "".join(out)


def dev_to_kannada(text):
    text = " ".join(delete_schwa(w) for w in text.replace("ाइ", "ै").split(" "))
    special = {"ॉ": "ಾ", "ॅ": "ೆ", "ऑ": "ಆ", "ऍ": "ಎ", "ँ": "ಂ", "़": "", "ड़": "ಡ", "ढ़": "ಢ"}
    out = []
    for ch in _decompose(text):
        if ch in special:
            out.append(special[ch])
        elif "\u0900" <= ch <= "\u097f":
            out.append(chr(ord(ch) + 0x380))
        else:
            out.append(ch)
    words = "".join(out).split(" ")
    fixed = []
    for w in words:
        if w and "\u0c95" <= w[-1] <= "\u0cb9":
            w += "್"
        fixed.append(w)
    return " ".join(fixed)


def dev_to_gurmukhi(text):
    special = {"ॉ": "ੌ", "ॅ": "ੈ", "ऑ": "ਆ", "ऍ": "ਐ", "ष": "ਸ਼", "ऋ": "ਰਿ", "ृ": "ਰਿ", "ड़": "ੜ", "ढ़": "ੜ", "ॆ": "ੇ", "ॊ": "ੋ"}
    src = _decompose(text).replace("ड़", "ड़").replace("ढ़", "ढ़").replace("ड़", "ड़").replace("ढ़", "ढ़")
    out = []
    i = 0
    while i < len(src):
        ch = src[i]
        if ch == VIRAMA:
            nxt = src[i + 1] if i + 1 < len(src) else ""
            prev = src[i - 1] if i else ""
            if nxt and nxt == prev:
                out[-1] = "ੱ"  # addak doubles the consonant that follows
            elif nxt in ("र", "व", "ह"):
                out.append("੍")
            i += 1
            continue
        if ch in ("ड", "ढ") and src[i + 1:i + 2] == "़":
            out.append("ੜ")
            i += 2
            continue
        if ch in ("ं", "ँ"):
            prev = src[i - 1] if i else ""
            out.append("ੰ" if prev in DEV_CONS or prev in ("ि", "ु", "अ", "इ", "उ") else "ਂ")
            i += 1
            continue
        if ch in special:
            out.append(special[ch])
        elif "\u0900" <= ch <= "\u097f":
            out.append(chr(ord(ch) + 0x100))
        else:
            out.append(ch)
        i += 1
    return "".join(out)


def dev_to_bengali(text):
    special = {"ॉ": "", "ॅ": "ে", "ऑ": "অ", "ऍ": "এ", "व": "ভ", "ळ": "ল", "ड़": "ড়", "ढ़": "ঢ়", "ॆ": "ে", "ॊ": "ো"}
    src = re.sub("ं(?=[तथदधनटठडढसजच])", "न्", _decompose(text))
    out = []
    for idx, ch in enumerate(src):
        prev = src[idx - 1] if idx else ""
        if ch == "़" and prev not in ("ड", "ढ"):
            continue
        if ch == "य" and prev and prev != VIRAMA:
            out.append("য়")
        elif ch == "व" and prev == VIRAMA:
            out.append("ব")
        elif ch in special:
            out.append(special[ch])
        elif "\u0900" <= ch <= "\u097f":
            out.append(chr(ord(ch) + 0x80))
        else:
            out.append(ch)
    return "".join(out)


def convert_dev(dev, lang):
    if lang in ("hi", "mr"):
        return dev
    if lang == "kn":
        return dev_to_kannada(dev)
    if lang == "pa":
        return dev_to_gurmukhi(dev)
    return dev_to_bengali(dev)


# ---------------------------------------------------------------- name translation

UNITS = {"g", "gm", "gms", "kg", "kgs", "ml", "l", "lt", "ltr", "ltrs", "n", "pcs", "pc", "oz", "x"}
TOKEN_RE = re.compile(r"\d+(?:[.,]\d+)?[A-Za-z]*|[A-Za-z][A-Za-z']*[A-Za-z]|[A-Za-z]|&|[^\sA-Za-z\d]")
CAMEL_RE = re.compile(r"(?<=[a-z])(?=[A-Z])")


def word_dev(token):
    low = token.lower()
    if low in DEV_TABLE:
        return DEV_TABLE[low]
    bare = low[:-2] if low.endswith("'s") else low
    if bare in DEV_TABLE:
        return DEV_TABLE[bare] + "्स" if low != bare else DEV_TABLE[bare]
    if len(token) == 1 or token.isupper() and len(token) <= 4:
        return "".join(LETTER_NAMES.get(c, c) for c in low)
    return to_devanagari(bare.replace("'", "").replace(".", ""))


def translate_name(name):
    """English product name -> {lang: name}."""
    tokens = TOKEN_RE.findall(name)
    result = {lang: [] for lang in LANGS}
    i = 0
    while i < len(tokens):
        tok = tokens[i]
        done = False
        for n in range(min(MAX_PHRASE_WORDS, len(tokens) - i), 1, -1):
            key = " ".join(t.lower() for t in tokens[i:i + n])
            key_dash = "".join(t.lower() for t in tokens[i:i + n])
            hit = PHRASE_TABLE.get(key) or PHRASE_TABLE.get(key_dash)
            if hit:
                for lang in LANGS:
                    result[lang].append(hit[lang])
                i += n
                done = True
                break
        if done:
            continue
        low = tok.lower()
        if low in WORD_TABLE:
            for lang in LANGS:
                if WORD_TABLE[low][lang]:
                    result[lang].append(WORD_TABLE[low][lang])
        elif any(c.isdigit() for c in tok) or not re.search(r"[A-Za-z]", tok) or low in UNITS:
            for lang in LANGS:
                result[lang].append(tok)
        else:
            parts = [tok] if low in DEV_TABLE or tok.isupper() else CAMEL_RE.split(tok)
            for part in parts:
                plow = part.lower()
                if len(parts) > 1 and plow in WORD_TABLE:
                    for lang in LANGS:
                        result[lang].append(WORD_TABLE[plow][lang])
                    continue
                dev = word_dev(part)
                for lang in LANGS:
                    result[lang].append(convert_dev(dev, lang))
        i += 1
    out = {}
    for lang, parts in result.items():
        text = " ".join(parts)
        text = re.sub(r"\s+([,.)\]])", r"\1", text)
        text = re.sub(r"([(\[])\s+", r"\1", text)
        text = re.sub(r"\s*-\s*", "-", text)
        out[lang] = re.sub(r"\s{2,}", " ", text).strip()
    return out


# ---------------------------------------------------------------- commands

def cmd_preview(args):
    if args.name:
        names = [args.name]
    else:
        with open(CACHE_FILE, encoding="utf-8") as f:
            names = [r["name"] for r in json.load(f)][: args.limit]
    for name in names:
        tr = translate_name(name)
        print(name)
        for lang in LANGS:
            print(f"   {lang}: {tr[lang]}")


def _push(api, path, rows, overwrite, label):
    """rows: [{id, name, translations}] -> one request per language per chunk."""
    for lang in LANGS:
        items = []
        for r in rows:
            if not overwrite and (r.get("translations") or {}).get(lang, "").strip():
                continue
            value = translate_name(r["name"]).get(lang, "").strip()
            if value:
                items.append({"id": r["id"], "name": value})
        for start in range(0, len(items), CHUNK):
            chunk = items[start:start + CHUNK]
            res = api.post_json(path, {"language": lang, "items": chunk})
            print(f"  {label} {lang}: {start + len(chunk)}/{len(items)} (updated {res.get('updated', 0)})", flush=True)


def cmd_apply(args):
    from seed_master_catalog import Api

    api = Api(args.api)
    masters = api.get_json("/master/products")
    print(f"master products: {len(masters)}", flush=True)
    _push(api, "/admin/master/products/translations", masters, args.overwrite, "master")
    if args.skip_shops:
        return
    for shop in api.get_json("/admin/shops"):
        sid = shop["id"]
        rows, page = [], 0
        while True:
            q = urllib.parse.urlencode({"page": page, "pageSize": 50, "status": "all"})
            data = api.get_json(f"/admin/shops/{urllib.parse.quote(sid)}/products?{q}")
            rows += data.get("items", [])
            if not data.get("hasMore"):
                break
            page += 1
        print(f"shop {sid}: {len(rows)} products", flush=True)
        _push(api, f"/admin/shops/{urllib.parse.quote(sid)}/products/translations", rows, args.overwrite, sid)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    p = sub.add_parser("preview")
    p.add_argument("--limit", type=int, default=30)
    p.add_argument("--name", default="")
    a = sub.add_parser("apply")
    a.add_argument("--api", default=os.environ.get("PANSARI_API", "https://api.pansariwala.shop"))
    a.add_argument("--overwrite", action="store_true", help="replace existing translations too")
    a.add_argument("--skip-shops", action="store_true", help="master catalog only")
    args = ap.parse_args()
    {"preview": cmd_preview, "apply": cmd_apply}[args.cmd](args)


if __name__ == "__main__":
    main()
