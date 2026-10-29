/**
 * Application Constants and Enums
 * Directly aligned with DisasterReliefNet Spring Boot Backend
 */

export const UserRole = {
  ADMIN: 'ADMIN',
  COORDINATOR: 'COORDINATOR',
  DONOR: 'DONOR',
  RECEIVER: 'RECEIVER',
  DRIVER: 'DRIVER',
};

export const DonationType = {
  MONETARY: 'MONETARY',
  IN_KIND: 'IN_KIND',
};

export const DonationStatus = {
  REGISTERED: 'REGISTERED',
  PLEDGED: 'PLEDGED',
  RECEIVED: 'RECEIVED',
  ALLOCATED: 'ALLOCATED',
  CANCELLED: 'CANCELLED',
};

export const AidCategory = {
  FOOD: 'FOOD',
  WATER: 'WATER',
  MEDICINE: 'MEDICINE',
  SHELTER: 'SHELTER',
  CLOTHING: 'CLOTHING',
  HYGIENE: 'HYGIENE',
  LOGISTICS: 'LOGISTICS',
  SEARCH_AND_RESCUE: 'SEARCH_AND_RESCUE',
  OTHER: 'OTHER',
};

export const UrgencyLevel = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  CRITICAL: 'CRITICAL',
};

export const RequestStatus = {
  SUBMITTED: 'SUBMITTED',
  VERIFIED: 'VERIFIED',
  ALLOCATED: 'ALLOCATED',
  IN_TRANSIT: 'IN_TRANSIT',
  FULFILLED: 'FULFILLED',
  CANCELLED: 'CANCELLED',
  REJECTED: 'REJECTED',
};

export const AllocationStatus = {
  SUGGESTED: 'SUGGESTED',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  FULFILLED: 'FULFILLED',
  CANCELLED: 'CANCELLED',
};

export const ShipmentStatus = {
  CREATED: 'CREATED',
  ASSIGNED: 'ASSIGNED',
  PICKED_UP: 'PICKED_UP',
  IN_TRANSIT: 'IN_TRANSIT',
  DELIVERED: 'DELIVERED',
  FAILED: 'FAILED',
  DISPUTED: 'DISPUTED',
  CANCELLED: 'CANCELLED',
};

export const DisasterType = {
  FLOOD: 'FLOOD',
  EARTHQUAKE: 'EARTHQUAKE',
  HURRICANE: 'HURRICANE',
  WILDFIRE: 'WILDFIRE',
  DROUGHT: 'DROUGHT',
  CONFLICT: 'CONFLICT',
  OTHER: 'OTHER',
};

export const SeverityLevel = {
  MINOR: 'MINOR',
  MODERATE: 'MODERATE',
  SEVERE: 'SEVERE',
  CATASTROPHIC: 'CATASTROPHIC',
};

export const DisasterStatus = {
  ACTIVE: 'ACTIVE',
  MONITORING: 'MONITORING',
  CLOSED: 'CLOSED',
};

export const CaseStatus = {
  OPEN: 'OPEN',
  UNDER_REVIEW: 'UNDER_REVIEW',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
};

export const VehicleType = {
  MOTORCYCLE: 'MOTORCYCLE',
  VAN: 'VAN',
  TRUCK: 'TRUCK',
  BOAT: 'BOAT',
  HELICOPTER: 'HELICOPTER',
};
