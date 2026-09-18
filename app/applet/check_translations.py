import xml.etree.ElementTree as ET
import glob, re, os

# Parse default strings
default_tree = ET.parse('app/src/main/res/values/strings.xml')
default_map = {}
for elem in default_tree.getroot().findall('string'):
    name = elem.attrib.get('name')
    default_map[name] = (elem.text or '', elem.attrib)

print(f"Total default strings: {len(default_map)}")

# List all other language files
lang_files = sorted(glob.glob('app/src/main/res/values-*/strings.xml'))
print(f"Found {len(lang_files)} localized string files:")
for lf in lang_files:
    ltree = ET.parse(lf)
    lmap = {e.attrib.get('name'): (e.text or '', e.attrib) for e in ltree.getroot().findall('string')}
    missing = set(default_map.keys()) - set(lmap.keys())
    print(f"  {lf}: {len(lmap)} strings, missing {len(missing)} keys: {sorted(list(missing))[:5]}")
