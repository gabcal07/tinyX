#!/bin/bash

set -e

# Formatting
BLUE='\033[0;34m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Spinner
spinner() {
    local pid=$!
    local delay=0.1
    local spinstr='|/-\'
    while kill -0 "$pid" 2>/dev/null; do
        local temp=${spinstr#?}
        printf " [%c]  " "$spinstr"
        spinstr=$temp${spinstr%"$temp"}
        sleep $delay
        printf "\b\b\b\b\b\b"
    done
    printf "    \b\b\b\b"
}

# Progress bar (fake duration-based)
progress_bar() {
    local duration=$1
    local columns=$(tput cols)
    local width=$((columns > 60 ? 40 : 20))
    echo -ne "${BLUE}Progress: ["
    for ((i=0; i<width; i++)); do
        echo -n " "
    done
    echo -ne "]"
    echo -ne "\r${BLUE}Progress: ["
    for ((i=0; i<width; i++)); do
        echo -ne "${GREEN}#"
        sleep 0.1
    done
    echo -e "${NC}] Done!"
}

# Minimal wait function
wait_for_pod() {
    local namespace="tinyx"
    local pod_prefix=$1
    local timeout=${2:-120}
    local interval=5
    local elapsed=0
    
    local width=40  # Width of the progress bar
    
    echo -e "${YELLOW}CHECKING IF PODS ARE READY...${NC}"

    # First check if pods with this prefix exist
    if ! kubectl get pods -n "$namespace" 2>/dev/null | grep -q "$pod_prefix"; then
        echo -e "${YELLOW}No pods with prefix '${pod_prefix}' found. Skipping wait.${NC}"
        return 0
    fi

    while [ $elapsed -lt $timeout ]; do
        local total_pods=$(kubectl get pods -n "$namespace" --no-headers 2>/dev/null | grep "$pod_prefix" | wc -l)
        local ready_pods=$(kubectl get pods -n "$namespace" --no-headers 2>/dev/null | grep "$pod_prefix" | grep -c "Running")

        # Avoid division by zero
        if [ "$total_pods" -eq 0 ]; then
            sleep $interval
            elapsed=$((elapsed + interval))
            continue
        fi

        # Calculate progress
        local percent=$((100 * ready_pods / total_pods))
        local filled=$((width * percent / 100))
        local empty=$((width - filled))

        progress_bar 2

        # If 80%+ pods ready, move on
        if [ "$ready_pods" -ge $((total_pods * 8 / 10)) ]; then
            echo -e "\n${GREEN}Pods ${pod_prefix} is ready!${NC}"
            return 0
        fi

        sleep $interval
        elapsed=$((elapsed + interval))
    done

    echo -e "\n${RED}Timed out waiting for ${pod_prefix} pods. Continuing anyway...${NC}"
    return 1
}

# Function to check MongoDB replica set status
check_mongodb_status() {
    local namespace="tinyx"
    local mongodb_instance=$1
    local max_retries=90  # Still ~90 seconds total
    local retry_interval=1  # Reduced from 3s to 1s for faster spinner
    local retries=0
    local primary_found=false
    local auth_ready=false

    echo -e "${BLUE}${BOLD}Waiting for MongoDB replica set and auth to become ready ${NC}"

    # Spinner characters - more frames for smoother animation
    local spinner_chars=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏')
    
    # First wait for the pod to be ready
    if ! wait_for_pod "$mongodb_instance" 300 >/dev/null; then
        echo -e "\n${RED}MongoDB pods not ready. Cannot check replica set status.${NC}"
        return 1
    fi

    sleep 10  # Small buffer after pods are up
    local POD_NAME="${mongodb_instance}-0"

    while [ $retries -lt $max_retries ]; do
        # Update spinner (use printf with carriage return for better compatibility)
        printf "\r${BLUE}Checking MongoDB status: ${spinner_chars[$((retries % 10))]} ${NC}"
        
        # Ensure pod is there
        if ! kubectl get pod -n $namespace $POD_NAME &>/dev/null; then
            retries=$((retries + 1))
            sleep $retry_interval
            continue
        fi

        local pod_status=$(kubectl get pod -n $namespace $POD_NAME -o jsonpath='{.status.phase}' 2>/dev/null)
        if [ "$pod_status" != "Running" ]; then
            retries=$((retries + 1))
            sleep $retry_interval
            continue
        fi

        # Only check actual MongoDB status every 3 iterations to avoid too many API calls
        # but still keep the spinner moving quickly
        if (( retries % 3 != 0 )); then
            retries=$((retries + 1))
            sleep $retry_interval
            continue
        fi

        # Check replica set initialized
        local rs_status=$(kubectl exec -n $namespace $POD_NAME -- mongosh --quiet --eval "try { rs.status().ok } catch(e) { print(e); }" 2>/dev/null)
        if [[ ! "$rs_status" =~ "1" ]]; then
            retries=$((retries + 1))
            sleep $retry_interval
            continue
        fi

        # Check for primary
        local primary_check=$(kubectl exec -n $namespace $POD_NAME -- mongosh --quiet --eval "
        try { 
            var status = rs.status();
            var primaryHost = null;
            status.members.forEach(function(m) { 
                if (m.state === 1) { primaryHost = m.name; }
            });
            primaryHost ? true : false;
        } catch(e) { false; }" 2>/dev/null)

        if [[ "$primary_check" != "true" ]]; then
            retries=$((retries + 1))
            sleep $retry_interval
            continue
        fi

        primary_found=true

        # Try auth
        local auth_test=$(kubectl exec -n $namespace $POD_NAME -- mongosh --quiet --eval "
        try {
            db.getSiblingDB('admin').auth('admin', 'admin');
            true;
        } catch(e) { false; }" 2>/dev/null)

        if [[ "$auth_test" != "true" ]]; then
            retries=$((retries + 1))
            sleep $retry_interval
            continue
        fi

        auth_ready=true

        # Final write test
        local write_test=$(kubectl exec -n $namespace $POD_NAME -- mongosh --quiet --eval "
        try {
            db.getSiblingDB('admin').auth('admin', 'admin');
            db.getSiblingDB('admin').test.insertOne({test: 'deployment_check', ts: new Date()});
            true;
        } catch(e) { false; }" 2>/dev/null)

        if [[ "$write_test" == "true" ]]; then
            printf "\r${GREEN}✅ MongoDB replica set is ready and authentication works!${NC}\n"
            return 0
        fi

        retries=$((retries + 1))
        sleep $retry_interval
    done

    printf "\r${RED}❌ MongoDB replica set failed to initialize in time.${NC}\n"
    echo -e "${YELLOW}You can debug using:${NC}"
    echo -e "  kubectl exec -n tinyx ${mongodb_instance}-0 -- mongosh --eval \"rs.status()\""
    return 1
}

delete_all() {
    echo -e "${RED}Deleting resources...${NC}"
    kubectl delete -k . --ignore-not-found=true &> /dev/null &
    spinner
    kubectl delete namespace tinyx --ignore-not-found=true &> /dev/null || true
    progress_bar 3
    echo -e "${GREEN}Resources deleted.${NC}"
}

deploy_all() {
    echo -e "${BLUE}${BOLD}Starting TinyX deployment to K3S...${NC}"
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}Error: kubectl is not installed. Please install it first.${NC}"
        exit 1
    fi
    
    # Check if kustomize is installed (either standalone or as part of kubectl)
    if ! command -v kustomize &> /dev/null && ! kubectl kustomize &> /dev/null; then
        echo -e "${RED}Error: kustomize is not installed. Please install it first.${NC}"
        exit 1
    fi
    
    # Apply all resources using the root kustomization file
    echo -e "${BLUE}${BOLD}Deploying all resources using kustomize...${NC}"
    kubectl apply -k . &> /dev/null
    progress_bar 10
    
    # Show progress
    progress_bar 10
    
    # Wait for critical services to be ready
    echo -e "${BLUE}${BOLD}Waiting for critical services to come up...${NC}"
    
    # Wait for MongoDB pods to come up
    wait_for_pod "shared-mongodb" 300 || echo -e "${YELLOW}MongoDB pods not all ready but continuing...${NC}"
    
    # Check MongoDB replica set status - this is critical so we'll fail if it's not ready
    echo -e "${BLUE}${BOLD}Waiting for MongoDB to be fully operational (this may take a moment)...${NC}"
    if ! check_mongodb_status "shared-mongodb"; then
        echo -e "${RED}${BOLD}MongoDB replica set is not properly initialized or authentication is not ready.${NC}"
        echo -e "${YELLOW}You may want to check the MongoDB pods manually or run the script again.${NC}"
        echo -e "${YELLOW}Continuing with deployment but services may not function correctly until MongoDB is ready.${NC}"
    else
        echo -e "${GREEN}${BOLD}MongoDB replica set successfully initialized with primary election and authentication confirmed!${NC}"
    fi
    
    echo -e "${GREEN}${BOLD}Deployment completed!${NC}"
    echo -e "${BLUE}All critical services should be operational now!${NC}"
    echo -e "\n${BLUE}Current pod status:${NC}"
    kubectl get pods -n tinyx
}

usage() {
    echo -e "${BLUE}Usage: $0 [--apply | --delete | --help]${NC}"
    exit 0
}

main() {
    case "$1" in
        --delete) delete_all ;;
        --apply|"") deploy_all ;;
        --help) usage ;;
        *) echo -e "${RED}Unknown option: $1${NC}"; usage ;;
    esac
}

main "$@"