#!/usr/bin/env python3

import requests
from bs4 import BeautifulSoup
import time
import json
import sys
import re
from datetime import datetime
from urllib.parse import urljoin

def get_all_javaparser_artifacts():
    """Get all available JavaParser artifacts from the main page"""
    url = "https://mvnrepository.com/artifact/com.github.javaparser"

    print("🔍 Fetching all JavaParser artifacts from mvnrepository.com...")

    try:
        response = requests.get(url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        response.raise_for_status()

        soup = BeautifulSoup(response.text, 'html.parser')

        artifacts = []

        # Find all artifact links on the page
        # They're in a div with class "im" inside links
        artifact_divs = soup.find_all('div', class_='im')

        for div in artifact_divs:
            link = div.find_parent('a')
            if link and 'href' in link.attrs:
                # Extract artifact name from URL
                href = link['href']
                if '/artifact/com.github.javaparser/' in href:
                    artifact_name = href.split('/')[-1]
                    if artifact_name and not artifact_name.startswith('?'):
                        artifacts.append(artifact_name)

        # Alternative method: look for links with specific pattern
        if not artifacts:
            links = soup.find_all('a', href=re.compile(r'/artifact/com\.github\.javaparser/[^/]+$'))
            for link in links:
                artifact_name = link['href'].split('/')[-1]
                if artifact_name and artifact_name not in artifacts:
                    artifacts.append(artifact_name)

        return sorted(list(set(artifacts)))  # Remove duplicates and sort

    except Exception as e:
        print(f"❌ Error fetching artifacts list: {e}")
        return []

def get_latest_version(artifact_id):
    """Fetch the latest version for a specific artifact"""
    url = f"https://mvnrepository.com/artifact/com.github.javaparser/{artifact_id}"

    try:
        response = requests.get(url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        })
        response.raise_for_status()

        soup = BeautifulSoup(response.text, 'html.parser')

        # Look for version buttons with "release" class
        version_links = soup.find_all('a', class_='vbtn')
        for link in version_links:
            if 'release' in link.get('class', []):
                version = link.text.strip()
                if version and re.match(r'^\d+\.\d+\.\d+', version):
                    return version

        # Alternative: look for any vbtn
        if version_links:
            version = version_links[0].text.strip()
            if version and re.match(r'^\d+\.\d+\.\d+', version):
                return version

        # Another alternative: look in the versions table
        version_elem = soup.find('a', class_='vnum')
        if version_elem:
            version = version_elem.text.strip()
            if version and re.match(r'^\d+\.\d+\.\d+', version):
                return version

        return None

    except Exception as e:
        return None

def main():
    print("🚀 JavaParser Complete Version Checker")
    print("=" * 50)
    print()

    # Get all artifacts
    artifacts = get_all_javaparser_artifacts()

    if not artifacts:
        print("❌ Could not find any JavaParser artifacts!")
        return

    print(f"📦 Found {len(artifacts)} JavaParser artifacts")
    print()

    group_id = "com.github.javaparser"
    versions = {}

    # Check each artifact
    for i, artifact in enumerate(artifacts, 1):
        print(f"[{i}/{len(artifacts)}] Checking {artifact}...", end='', flush=True)
        version = get_latest_version(artifact)

        if version:
            versions[artifact] = version
            print(f" ✅ {version}")
        else:
            print(" ⚠️  No stable version found")

        # Be nice to the server
        time.sleep(0.3)

    # Summary
    print("\n" + "=" * 50)
    print("📊 Version Summary:")
    print("=" * 50)

    if not versions:
        print("❌ No versions found!")
        return

    # Group by version
    version_groups = {}
    for artifact, version in versions.items():
        if version not in version_groups:
            version_groups[version] = []
        version_groups[version].append(artifact)

    print(f"\n📈 Found {len(version_groups)} different versions:\n")

    for version in sorted(version_groups.keys(), reverse=True):
        artifacts_with_version = version_groups[version]
        print(f"📌 Version {version}: {len(artifacts_with_version)} artifacts")
        for artifact in sorted(artifacts_with_version):
            print(f"   - {artifact}")
        print()

    # Generate Maven XML
    print("\n" + "=" * 50)
    print("📝 Complete Maven Dependencies XML:")
    print("=" * 50)

    xml_output = f"""<?xml version="1.0" encoding="UTF-8"?>
<!-- JavaParser Complete Dependencies List -->
<!-- Generated on {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} -->
<!-- Found {len(versions)} artifacts with versions -->

<dependencies>"""

    for artifact, version in sorted(versions.items()):
        xml_output += f"""
    <dependency>
        <groupId>{group_id}</groupId>
        <artifactId>{artifact}</artifactId>
        <version>{version}</version>
    </dependency>"""

    xml_output += "\n</dependencies>"
    print(xml_output)

    # Save reports
    timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')

    # JSON report
    json_file = f"javaparser-all-versions-{timestamp}.json"
    with open(json_file, 'w') as f:
        json.dump({
            "generated": datetime.now().isoformat(),
            "artifacts_count": len(versions),
            "versions": versions,
            "version_groups": version_groups
        }, f, indent=2)
    print(f"\n📄 JSON report saved to: {json_file}")

    # XML file
    xml_file = f"javaparser-all-dependencies-{timestamp}.xml"
    with open(xml_file, 'w') as f:
        f.write(xml_output)
    print(f"📄 XML dependencies saved to: {xml_file}")

    # CSV report
    csv_file = f"javaparser-versions-{timestamp}.csv"
    with open(csv_file, 'w') as f:
        f.write("artifact,version\n")
        for artifact, version in sorted(versions.items()):
            f.write(f"{artifact},{version}\n")
    print(f"📄 CSV report saved to: {csv_file}")

    # Summary report
    summary_file = f"javaparser-summary-{timestamp}.txt"
    with open(summary_file, 'w') as f:
        f.write(f"JavaParser Artifacts Summary\n")
        f.write(f"Generated: {datetime.now()}\n")
        f.write(f"Total artifacts: {len(versions)}\n")
        f.write(f"Unique versions: {len(version_groups)}\n\n")

        for version in sorted(version_groups.keys(), reverse=True):
            f.write(f"\nVersion {version} ({len(version_groups[version])} artifacts):\n")
            for artifact in sorted(version_groups[version]):
                f.write(f"  - {artifact}\n")

    print(f"📄 Summary report saved to: {summary_file}")

    print(f"\n✅ Complete! Checked {len(artifacts)} artifacts, found {len(versions)} with versions")

if __name__ == "__main__":
    main()