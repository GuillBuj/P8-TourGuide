# 🗺️ TourGuide

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.x-green)
![Build Status](https://github.com/GuillBuj/P8-TourGuide/actions/workflows/ci.yml/badge.svg?branch=dev)
![License](https://img.shields.io/badge/license-Internal-lightgrey)

> A modern Java Spring Boot travel planning application that helps users discover nearby tourist attractions and benefit from discounts on hotels and events.

---

## 🚀 Live Artifacts

- 📥 **Latest JAR file**:  
  [⬇️ Download `TourGuide.jar`](https://github.com/GuillBuj/P8-TourGuide/releases/latest/download/TourGuide-1.0-SNAPSHOT.jar)

- 📈 **Code Coverage (JaCoCo)**:  
  [📊 View Report](https://GuillBuj.github.io/P8-TourGuide/jacoco/index.html)

- ✅ **Unit Test Report (Surefire)**:  
  [🧪 View Results](https://GuillBuj.github.io/P8-TourGuide/surefire/index.html)

- 📚 **Javadoc Documentation**:  
  [📘 Browse Javadoc](https://GuillBuj.github.io/P8-TourGuide/javadoc/index.html)

---

## ⚙️ Tech Stack

- ![Java](https://img.shields.io/badge/Java-17-orange)
- ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.x-green)
- JUnit 5
- Maven
- GitHub Actions (CI/CD)
- JaCoCo / Surefire / GitHub Pages

---

## 📁 Project Structure

<details>
<summary>Click to expand full file structure</summary>

```plaintext
src
├───main
│   ├───java
│   │   └───com
│   │       └───openclassrooms
│   │           └───tourguide
│   │               │   TourguideApplication.java
│   │               ├───config
│   │               │       TourGuideModule.java
│   │               ├───controller
│   │               │       TourGuideController.java
│   │               ├───dto
│   │               │       NearbyAttractionDTO.java
│   │               ├───helper
│   │               │       InternalTestHelper.java
│   │               ├───service
│   │               │       RewardsService.java
│   │               │       TourGuideService.java
│   │               ├───tracker
│   │               │       Tracker.java
│   │               └───user
│   │                       User.java
│   │                       UserPreferences.java
│   │                       UserReward.java
│   └───resources
│           application.properties

## 📦 Installing Local Dependencies

This project uses 3 external `.jar` libraries not hosted on Maven Central: `gpsUtil`, `RewardCentral`, and `TripPricer`.

Before building locally, install them into your local Maven repository:

```bash
mvn install:install-file -Dfile=libs/gpsUtil.jar -DgroupId=gpsUtil -DartifactId=gpsUtil -Dversion=1.0.0 -Dpackaging=jar

mvn install:install-file -Dfile=libs/RewardCentral.jar -DgroupId=rewardCentral -DartifactId=rewardCentral -Dversion=1.0.0 -Dpackaging=jar

mvn install:install-file -Dfile=libs/TripPricer.jar -DgroupId=tripPricer -DartifactId=tripPricer -Dversion=1.0.0 -Dpackaging=jar
🧪 Running Locally
bash
Copier le code
git clone https://github.com/GuillBuj/P8-TourGuide.git
cd P8-TourGuide

# Build project and skip tests if needed
mvn clean package -DskipTests

# Launch the app
java -jar target/TourGuide-1.0-SNAPSHOT.jar
📁 Project Structure

🔄 CI/CD Automation
GitHub Actions automatically handles:

✅ Code compilation and unit testing

🧪 Code coverage (JaCoCo) and test reports (Surefire)

📚 Javadoc generation

📦 .jar packaging and publishing to GitHub Releases

🌐 Deployment to GitHub Pages (gh-pages branch)
